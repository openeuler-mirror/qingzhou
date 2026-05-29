package qingzhou.http.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import qingzhou.http.server.HttpRequest;
import reactor.netty.http.server.HttpServerRequest;

class HttpRequestImpl implements HttpRequest {
    private final HttpServerRequest request;
    private final String requestPath;

    private byte[] requestBody;

    private QueryStringDecoder queryStringDecoder;

    HttpRequestImpl(HttpServerRequest request, String requestPath) {
        this.request = request;
        this.requestPath = requestPath;
    }

    void setRequestBody(byte[] requestBody) {
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
        return queryStringDecoder.parameters();
    }

    @Override
    public byte[] getBody() {
        return requestBody;
    }
}
