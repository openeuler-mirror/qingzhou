package qingzhou.http.server.impl;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import qingzhou.http.server.HttpRequest;
import reactor.netty.http.server.HttpServerRequest;

public class HttpRequestImpl implements HttpRequest {
    private static final int MAX_FORM_PARAMS = 1000; // 表单参数个数上限：请求体可达 8MB，不限量会放大内存

    private final HttpServerRequest request;
    private final String requestPath;

    // 以下由框架/认证层在 EventLoop 写入，handler 可能在业务线程读取，须保证可见性
    private volatile byte[] requestBody;
    private volatile Map<String, List<String>> parameters;
    private final Object parameterLock = new Object();

    // 认证结果：由认证层写入，对外只暴露只读的强类型接口
    private final AtomicBoolean authSet = new AtomicBoolean();
    private volatile String authPrincipal;
    private volatile String[] authRoles;

    HttpRequestImpl(HttpServerRequest request, String requestPath) {
        this.request = request;
        this.requestPath = requestPath;
    }

    // body 更新与缓存失效须在同一临界区内：「缓存与 body 配对」的不变式不依赖调用方时序；
    // requestBody 为 volatile，锁内写不增加额外同步成本
    void setRequestBody(byte[] requestBody) {
        synchronized (parameterLock) {
            this.requestBody = requestBody;
            this.parameters = null; // 请求体变化后需重新解析
        }
    }

    public void setPrincipal(String principal, String[] roles) {
        if (!authSet.compareAndSet(false, true)) {
            throw new IllegalStateException("auth result already set");
        }

        authPrincipal = principal;
        authRoles = roles == null ? null : roles.clone();
    }

    @Override
    public String getRawUri() {
        return request.uri();
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress address = request.remoteAddress();
        return address != null ? address.getHostString() : "unknown";
    }

    @Override
    public String getPath() {
        return requestPath;
    }

    @Override
    public String getHeader(String header) {
        return request.requestHeaders().get(header);
    }

    @Override
    public String getContentType() {
        return request.requestHeaders().get(HttpHeaderNames.CONTENT_TYPE);
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    @Override
    public boolean isFormUrlencoded() {
        return request.isFormUrlencoded();
    }

    @Override
    public String getParameter(String name) {
        List<String> values = getParameters().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Map<String, List<String>> getParameters() {
        Map<String, List<String>> snapshot = parameters;
        if (snapshot == null) {
            // 解析一次即缓存：表单体最大 8MB，重复解析的代价远高于一次加锁
            synchronized (parameterLock) {
                snapshot = parameters;
                if (snapshot == null) {
                    parameters = snapshot = parseParameters();
                }
            }
        }
        return snapshot;
    }

    // 先在局部构建完成、再经 volatile 字段整体发布：并发读者只会看到空表或完整表，不会看到半填充表
    private Map<String, List<String>> parseParameters() {
        Map<String, List<String>> parsed = new LinkedHashMap<>(); // 保序：同名参数按 query 在前、表单体在后排列
        try {
            // 与表单体同阈值：query 侧不再依赖框架默认上限（升级即行为漂移）
            parsed.putAll(new QueryStringDecoder(request.uri(), StandardCharsets.UTF_8, true, MAX_FORM_PARAMS).parameters());
        } catch (IllegalArgumentException e) {
            // query 含非法 %xx 转义时按无参数处理，不能让参数解析中断请求
        }
        byte[] body = requestBody;
        if (body != null && body.length > 0 && isFormUrlencoded()) {
            try {
                // hasPath=false：表单体整段即 query，无需拼 "/?" 前缀（省掉一次与请求体等长的字符串分配）
                // 显式限制参数个数：请求体最大 8MB，不限量会被展开成百万条目并放大内存
                new QueryStringDecoder(new String(body, StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8, false, MAX_FORM_PARAMS).parameters()
                        .forEach((key, values) -> parsed.merge(key, values, (oldValues, newValues) -> {
                            List<String> merged = new ArrayList<>(oldValues);
                            merged.addAll(newValues);
                            return merged;
                        }));
            } catch (IllegalArgumentException e) {
                // 请求体声称是表单但实际无法解码（如密文二进制流），忽略即可，不能因此中断请求
            }
        }
        return Collections.unmodifiableMap(parsed); // 只读视图：防止调用方篡改已解析的参数
    }

    @Override
    public byte[] getBody() {
        return requestBody;
    }

    @Override
    public String getPrincipal() {
        return authPrincipal;
    }

    @Override
    public String[] getRoles() {
        return authRoles == null ? null : authRoles.clone(); // 副本：调用方改动不影响认证结果
    }
}
