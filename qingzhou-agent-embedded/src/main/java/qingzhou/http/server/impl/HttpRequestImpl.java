package qingzhou.http.server.impl;

import com.sun.net.httpserver.HttpExchange;
import qingzhou.http.server.HttpRequest;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestImpl implements HttpRequest {
    private final HttpExchange exchange;
    private final Map<String, List<String>> parameters;
    private byte[] body;

    HttpRequestImpl(HttpExchange exchange) {
        this.exchange = exchange;
        this.parameters = new HashMap<>();
        parseQueryString();
    }

    private void parseQueryString() {
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                String key = decodeUrl(pair[0]);
                String value = pair.length > 1 ? decodeUrl(pair[1]) : "";
                parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
    }

    private String decodeUrl(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress remote = exchange.getRemoteAddress();
        return remote != null ? remote.getHostString() : "unknown";
    }

    @Override
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    @Override
    public String getFullPath() {
        return exchange.getRequestURI().toString();
    }

    @Override
    public String getParameter(String name) {
        List<String> values = parameters.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    @Override
    public String getHeader(String header) {
        return exchange.getRequestHeaders().getFirst(header);
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public String getMethod() {
        return exchange.getRequestMethod();
    }

    @Override
    public boolean isFormUrlencoded() {
        String ct = getContentType();
        return ct != null && ct.contains("application/x-www-form-urlencoded");
    }

    @Override
    public byte[] getBody() {
        if (body == null) {
            try {
                InputStream is = exchange.getRequestBody();
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int n;
                while ((n = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, n);
                }
                body = buffer.toByteArray();
            } catch (Exception e) {
                body = new byte[0];
            }
        }
        return body;
    }
}