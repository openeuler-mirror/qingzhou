package qingzhou.http.impl;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import qingzhou.logger.Logger;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import static qingzhou.http.impl.ConfigUtil.atLeast;
import static qingzhou.http.impl.ConfigUtil.getConfig;

/**
 * 请求分发入口：装配安全头、准入闸门与 body 处理三条协作链，并持有许可的获取与归还。
 * 具体判定见 {@link RequestGates}，body 处理见 {@link BodyProcessors}，响应头见 {@link ResponseHeaders}。
 */
@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = DispatcherHandler.class)
public class DispatcherHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    @Reference
    private Logger logger;
    @Reference
    private AuthManager authManager;
    @Reference
    private HandlerManager handlerManager;

    // 全局聚合配额计数：final 引用跨配置更新共享，热更新重建闸门时在途预留仍归还到同一计数
    private final AtomicLong aggregatedBytes = new AtomicLong();

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性；协作对象在 init 时整体重建
    private volatile Semaphore concurrentSemaphore;
    private volatile Semaphore streamSemaphore;
    private volatile ResponseHeaders responseHeaders;
    private volatile RequestGates requestGates;
    private volatile BodyProcessors bodyProcessors;

    @Activate
    public void init(Map<String, String> config) {
        int maxConcurrentRequests = atLeast(getConfig(config, "max_concurrent_requests", 1000), 1, "max_concurrent_requests");
        this.concurrentSemaphore = new Semaphore(maxConcurrentRequests);
        // 流式上传不占堆内存配额，须单独限并发：否则大量并发上传会同时占满临时文件与连接
        int maxStreams = atLeast(getConfig(config, "max_concurrent_streams", Math.min(maxConcurrentRequests, 64)),
                1, "max_concurrent_streams");
        this.streamSemaphore = new Semaphore(maxStreams);
        this.responseHeaders = new ResponseHeaders(config);
        this.requestGates = new RequestGates(config, authManager, handlerManager, logger, aggregatedBytes);
        this.bodyProcessors = new BodyProcessors(config, logger);
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        responseHeaders.apply(request, response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return response.status(HttpResponseStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                    .send();
        }

        RequestContext ctx = new RequestContext(request, response, concurrentSemaphore, streamSemaphore);
        // 闸门判定同步执行：它有副作用（预留全局配额），放进 defer 会在重复订阅时被重复执行
        Publisher<Void> denial;
        try {
            denial = requestGates.check(ctx);
        } catch (Throwable e) {
            release(ctx);
            throw e;
        }
        // 延迟执行：业务处理同步抛异常时也能走到 doFinally，不至于泄漏许可与配额
        return Flux.defer(() -> denial != null ? denial : bodyProcessors.process(ctx))
                .doFinally(signal -> release(ctx));
    }

    // 许可与内存配额统一在入口归还：gate 只管判定，不必关心释放，也不受 gate 顺序影响
    private void release(RequestContext ctx) {
        if (!ctx.released.compareAndSet(false, true)) return; // 归还次数须与 tryAcquire 一致，重复订阅不得超发许可
        ctx.concurrentSemaphore.release();
        if (ctx.streamPermitHeld) ctx.streamSemaphore.release();
        if (ctx.reservedBytes > 0) aggregatedBytes.addAndGet(-ctx.reservedBytes);
    }
}
