package qingzhou.http.server.impl;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import qingzhou.common.Configs;
import qingzhou.http.server.auth.impl.AuthManager;
import qingzhou.logger.Logger;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * 请求分发入口：装配安全头、准入闸门与 body 处理三条协作链，并持有许可的获取与归还。
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

    // 由 OSGi 激活线程整体替换、Netty EventLoop 一次读取：各字段分别替换会让在途请求混用新旧配置
    private volatile HotConfig hotConfig;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        int maxConcurrentRequests = configs.atLeast("max_concurrent_requests", 1, 1000);
        Semaphore concurrentSemaphore = new Semaphore(maxConcurrentRequests);
        // 流式上传不占堆内存配额，须单独限并发：否则大量并发上传会同时占满临时文件与连接
        int maxStreams = configs.atLeast("max_concurrent_streams", 1, Math.min(maxConcurrentRequests, 64));
        Semaphore streamSemaphore = new Semaphore(maxStreams);

        long maxBodyBytes = configs.atLeast("max_body_bytes", 1L, 8 * 1024 * 1024);
        // 流式上传落临时文件，默认额度过大易打满磁盘
        long maxStreamBytes = configs.atLeast("max_stream_bytes", 1L, 256L * 1024 * 1024);
        // 逐来源并发上限：全局额度无来源维度，单 IP 洪水即可耗尽；0 表示不启用
        SourceLimiter sourceLimiter = new SourceLimiter(
                configs.atLeast("max_concurrent_per_source", 0, 64),
                configs.atLeast("max_tracked_sources", 1, 10_000),
                configs.atLeast("source_idle_seconds", 1, 300));
        // 请求进入期（准入到请求体读完）总时长上限：慢速滴漏请求被 idle_timeout 漏掉
        // （每秒 1 字节连接永远"活跃"），却可长期占住并发许可。0 表示不启用
        int intakeTimeoutSeconds = configs.atLeast("request_timeout_seconds", 0, 10);
        ResponseHeaders responseHeaders = new ResponseHeaders(config);
        RequestGates requestGates = new RequestGates(config, maxBodyBytes, maxStreamBytes, sourceLimiter, authManager, handlerManager, logger, aggregatedBytes);
        BodyProcessors bodyProcessors = new BodyProcessors(maxBodyBytes, maxStreamBytes, logger);

        hotConfig = new HotConfig(concurrentSemaphore, streamSemaphore, intakeTimeoutSeconds,
                responseHeaders, requestGates, bodyProcessors); // 最后一步替换：请求侧要么看到旧配置，要么看到新配置
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        HotConfig hot = hotConfig; // 单次读取快照：许可、安全头、闸门与 body 处理来自同一代配置
        hot.responseHeaders.apply(request, response); // 须在任何 return 之前：400/401/404/413/503 等分支同样需要安全头
        if (!hot.concurrentSemaphore.tryAcquire()) { // 请求体按上限聚合进内存，不限在途请求数则内存仍会耗尽
            return response.status(HttpResponseStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                    .send();
        }

        RequestContext ctx = new RequestContext(request, response, hot.concurrentSemaphore, hot.streamSemaphore);

        if (hot.intakeTimeoutSeconds > 0) scheduleIntakeTimeout(ctx, hot.intakeTimeoutSeconds);

        // 闸门判定同步执行：它有副作用（预留全局配额），放进 defer 会在重复订阅时被重复执行
        Publisher<Void> denial;
        try {
            denial = hot.requestGates.check(ctx);
        } catch (Throwable e) {
            release(ctx);
            throw e;
        }
        // 延迟执行：业务处理同步抛异常时也能走到 doFinally，不至于泄漏许可与配额
        return Flux.defer(() -> denial != null ? denial : hot.bodyProcessors.handle(ctx))
                .doFinally(signal -> release(ctx));
    }

    // 一次性替换的运行时配置：避免热更新期间在途请求混用新旧闸门/处理器
    private static final class HotConfig {
        final Semaphore concurrentSemaphore;
        final Semaphore streamSemaphore;
        final int intakeTimeoutSeconds;
        final ResponseHeaders responseHeaders;
        final RequestGates requestGates;
        final BodyProcessors bodyProcessors;

        HotConfig(Semaphore concurrentSemaphore, Semaphore streamSemaphore, int intakeTimeoutSeconds,
                  ResponseHeaders responseHeaders, RequestGates requestGates, BodyProcessors bodyProcessors) {
            this.concurrentSemaphore = concurrentSemaphore;
            this.streamSemaphore = streamSemaphore;
            this.intakeTimeoutSeconds = intakeTimeoutSeconds;
            this.responseHeaders = responseHeaders;
            this.requestGates = requestGates;
            this.bodyProcessors = bodyProcessors;
        }
    }

    /**
     * 请求进入期超时（防 Slowloris）：到时请求体仍未读完则强制断开。
     * 关闭连接会中止在途管道，doFinally 仍会归还许可与配额；进入期正常结束时由
     * {@link RequestContext#intakeComplete()}（聚合/流式读完）或 release()（拒绝/异常）取消，
     * 故 handler 执行期与 SSE 等长响应不受此约束。
     */
    private void scheduleIntakeTimeout(RequestContext ctx, int timeoutSeconds) {
        // 运行时实现类（ChannelOperations）本身实现 Connection 接口，由此取得底层 channel；
        // 实现细节随框架版本可能变化：不符合预期时跳过超时兜底（记录告警），不做脆弱强转
        if (!(ctx.request instanceof Connection)) {
            logger.warn("cannot schedule intake timeout: request is not a Connection: "
                    + ctx.request.getClass().getName());
            return;
        }
        Channel channel = ((Connection) ctx.request).channel();
        ctx.intakeTimeout = channel.eventLoop().schedule(() -> {
            if (ctx.released.get()) return; // 与 release() 的取消竞态：已结束的请求不再处理
            logger.warn("request intake exceeded " + timeoutSeconds + "s, closing connection: "
                    + ctx.request.method() + " " + safePath(ctx));
            channel.close();
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    // 只记录规范化路径：原始 URI 的 query 可能携带 password= 等敏感参数，落日志即泄露；
    // 超时触发时闸门已同步执行完毕（同一 IO 线程排队），path 必然已解析
    private static String safePath(RequestContext ctx) {
        return ctx.path != null ? ctx.path : "<unresolved>";
    }

    // 许可与内存配额统一在入口归还：gate 只管判定，不必关心释放，也不受 gate 顺序影响
    private void release(RequestContext ctx) {
        if (!ctx.released.compareAndSet(false, true)) return; // 归还次数须与 tryAcquire 一致，重复订阅不得超发许可
        ctx.intakeComplete(); // 拒绝分支/异常分支的进入期到此结束
        ctx.concurrentSemaphore.release();
        if (ctx.streamPermitHeld) ctx.streamSemaphore.release();
        if (ctx.sourceReservation != null) {
            ctx.sourceReservation.limiter.release(ctx.sourceReservation.key); // 归还到取得许可时的限制器实例
        }
        if (ctx.reservedBytes > 0) aggregatedBytes.addAndGet(-ctx.reservedBytes);
    }
}
