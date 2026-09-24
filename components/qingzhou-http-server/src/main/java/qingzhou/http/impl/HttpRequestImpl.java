package qingzhou.http.impl;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;
import reactor.netty.http.server.HttpServerRequest;

class HttpRequestImpl implements HttpRequest {
    private static final int MAX_FORM_PARAMS = 1000; // 表单参数个数上限：请求体可达 8MB，不限量会放大内存

    private final HttpServerRequest request;
    private final String requestPath;

    // 以下由框架/认证层在 EventLoop 写入，handler 可能在业务线程读取，须保证可见性
    private volatile byte[] requestBody;
    private volatile Map<String, List<String>> parameters;

    // 认证结果：由认证层写入，对外只暴露只读的强类型接口
    private volatile String authPrincipal;
    private volatile String[] authRoles;

    HttpRequestImpl(HttpServerRequest request, String requestPath) {
        this.request = request;
        this.requestPath = requestPath;
    }

    void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
        this.parameters = null; // 请求体变化后需重新解析
    }

    // 认证层写入口，由 AuthManager.doAuth0 在每请求内调用一次；不允许多次调用。
    // roles 在此处 clone，认证器后续改动其入参数组不影响已确定的认证结果
    void setAuth(AuthResult authResult) {
        this.authPrincipal = authResult.getPrincipal();
        String[] roles = authResult.getRoles();
        this.authRoles = roles == null ? null : roles.clone();
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
        Map<String, List<String>> parameters = getParameters();
        if (parameters != null) {
            List<String> list = parameters.get(name);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }

        return null;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        if (parameters == null) {
            parameters = new ConcurrentHashMap<>(); // 并发容器：handler 可能把请求交给业务线程，HashMap 并发 resize 会损坏
            try {
                parameters.putAll(new QueryStringDecoder(request.uri()).parameters());
            } catch (IllegalArgumentException e) {
                // query 含非法 %xx 转义时按无参数处理，不能让参数解析中断请求
            }
            if (requestBody != null && requestBody.length > 0 && isFormUrlencoded()) {
                try {
                    // 带路径前缀、默认解码（hasPath=true 时 "?x=y" 的 "?" 会被正确剥离）
                    // 显式限制参数个数：请求体最大 8MB，不限量会被展开成百万条目并放大内存
                    new QueryStringDecoder("/?" + new String(requestBody, StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8, true, MAX_FORM_PARAMS).parameters()
                            .forEach((key, values) -> parameters.merge(key, values, (oldValues, newValues) -> {
                                List<String> merged = new ArrayList<>(oldValues);
                                merged.addAll(newValues);
                                return merged;
                            }));
                } catch (IllegalArgumentException e) {
                    // 请求体声称是表单但实际无法解码（如密文二进制流），忽略即可，不能因此中断请求
                }
            }
        }
        return Collections.unmodifiableMap(parameters); // 只读视图：防止调用方篡改已解析的参数
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
