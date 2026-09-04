package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import qingzhou.http.server.HttpRequest;
import reactor.netty.http.server.HttpServerRequest;

class HttpRequestImpl implements HttpRequest {
    private final HttpServerRequest request;
    private final String requestPath;

    private byte[] requestBody;
    private Map<String, List<String>> parameters;
    private Map<String, Object> attributes;

    HttpRequestImpl(HttpServerRequest request, String requestPath) {
        this.request = request;
        this.requestPath = requestPath;
    }

    void setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
        this.parameters = null; // 请求体变化后需重新解析
    }

    @Override
    public String getFullPath() {
        return request.uri();
    }

    @Override
    public String getRemoteHost() {
        return request.remoteAddress() != null ?
                Objects.requireNonNull(request.remoteAddress()).getHostString() :
                "unknown";
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
            parameters = new HashMap<>(new QueryStringDecoder(request.uri()).parameters());
            if (requestBody != null && requestBody.length > 0 && isFormUrlencoded()) {
                try {
                    // 带路径前缀、默认解码（hasPath=true 时 "?x=y" 的 "?" 会被正确剥离）
                    new QueryStringDecoder("/?" + new String(requestBody, StandardCharsets.UTF_8)).parameters()
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
        return parameters;
    }

    @Override
    public byte[] getBody() {
        return requestBody;
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (attributes == null) attributes = new HashMap<>();
        attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }
}
