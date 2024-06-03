package qingzhou.http.impl;

import qingzhou.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponseImpl implements HttpResponse {
    private final int code;
    private final String result;
    private final Map<String, List<String>> headers;

    private final HttpURLConnection conn;

    public HttpResponseImpl(HttpURLConnection conn) throws IOException {
        code = conn.getResponseCode();
        result = inputStreamToString(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
        headers = new HashMap<>();
        this.conn = conn;
        conn.getHeaderFields().forEach((s, strings) -> headers.put(s, new ArrayList<>(strings)));
    }

    @Override
    public String getResponseBody() {
        try {
            return result;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public int getResponseCode() {
        return code;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return headers;
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        int len;
        byte[] bytes = new byte[1024 * 8];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while ((len = inputStream.read(bytes)) != -1) {
            os.write(bytes, 0, len);
        }
        return os.toString("UTF-8");
    }
}
