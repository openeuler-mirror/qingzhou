package qingzhou.http.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import qingzhou.http.server.HttpRequest;
import reactor.netty.http.server.HttpServerRequest;

public class HttpRequestImpl implements HttpRequest {
    private final HttpServerRequest request;
    private final String requestPath;
    private final byte[] requestBody;

    private QueryStringDecoder queryStringDecoder;

    HttpRequestImpl(HttpServerRequest request,
                    String requestPath, byte[] requestBody) {
        this.request = request;
        this.requestPath = requestPath;
        this.requestBody = requestBody;
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
        if (queryStringDecoder == null) {
            queryStringDecoder = new QueryStringDecoder(request.uri());
        }
        Map<String, List<String>> parameters = queryStringDecoder.parameters();

        // 如果是 POST 请求且 Content-Type 为 application/x-www-form-urlencoded，需要从请求体中解析参数
        if (request.method() == HttpMethod.POST
                && isFormUrlencoded()
                && requestBody != null && requestBody.length > 0) {
            QueryStringDecoder bodyDecoder = new QueryStringDecoder("/?" + new String(requestBody, StandardCharsets.UTF_8));
            Map<String, List<String>> bodyParams = bodyDecoder.parameters();

            // 合并 URL 参数和表单参数
            for (Map.Entry<String, List<String>> entry : bodyParams.entrySet()) {
                parameters.merge(entry.getKey(), entry.getValue(), (oldList, newList) -> {
                    oldList.addAll(newList);
                    return oldList;
                });
            }
        }

        return parameters;
    }

    @Override
    public byte[] getBody() {
        return requestBody;
    }
}
