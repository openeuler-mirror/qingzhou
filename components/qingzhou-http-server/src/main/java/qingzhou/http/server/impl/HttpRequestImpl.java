package qingzhou.http.server.impl;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
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
        // 契约适配：连接尚未完全建立时 remoteAddress 可为 null，锁定/限流按固定兜底键归并
        return address != null ? address.getHostString() : "unknown";
    }

    @Override
    public String getPath() {
        return requestPath;
    }

    @Override
    public String getHeader(String name) {
        io.netty.handler.codec.http.HttpHeaders headers = request.requestHeaders();
        return headers.get(name);
    }

    @Override
    public String getCookie(String name) {
        Map<CharSequence, List<Cookie>> allCookies = request.allCookies();
        List<Cookie> cookies = allCookies.get(name);
        return cookies == null || cookies.isEmpty() ? null : cookies.get(0).value();
    }

    @Override
    public String getContentType() {
        io.netty.handler.codec.http.HttpHeaders headers = request.requestHeaders();
        return headers.get(HttpHeaderNames.CONTENT_TYPE);
    }

    @Override
    public String getScheme() {
        return request.scheme();
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
        Map<String, List<String>> parsed = new LinkedHashMap<>(decodeParams(request.uri(), true)); // 保序：query 在前、表单体在后
        byte[] body = requestBody;
        if (body != null && body.length > 0 && isFormUrlencoded()) {
            // hasPath=false：表单体整段即 query，无需拼 "/?" 前缀（省掉一次与请求体等长的字符串分配）
            decodeParams(new String(body, StandardCharsets.UTF_8), false)
                    .forEach((key, values) -> parsed.merge(key, values, (oldValues, newValues) -> {
                        List<String> merged = new ArrayList<>(oldValues);
                        merged.addAll(newValues);
                        return merged;
                    }));
        }
        // 深只读视图：Map 与内层 List 均不可变，防止调用方篡改已解析的参数
        Map<String, List<String>> readOnly = new LinkedHashMap<>(parsed.size());
        parsed.forEach((key, values) -> readOnly.put(key, Collections.unmodifiableList(values)));
        return Collections.unmodifiableMap(readOnly);
    }

    // Netty 超限时静默截断（不抛异常），故以「上限+1」为探针解析：参数对数量越界即整体丢弃；
    // 截断按参数对计数（同名重复亦占额），需累加各 key 的 value 数而非只数 key
    private static Map<String, List<String>> decodeParams(String raw, boolean hasPath) {
        try {
            Map<String, List<String>> params = new QueryStringDecoder(raw, StandardCharsets.UTF_8, hasPath,
                    MAX_FORM_PARAMS + 1).parameters();
            int count = 0;
            for (List<String> values : params.values()) {
                if ((count += values.size()) > MAX_FORM_PARAMS) {
                    return Collections.emptyMap();
                }
            }
            return params;
        } catch (IllegalArgumentException | TooLongFrameException e) {
            // 非法 %xx 转义或解码器版本差异导致超限抛异常：按无参数处理，不能中断请求
            return Collections.emptyMap();
        }
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
