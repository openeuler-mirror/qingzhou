package qingzhou.http.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.http.HttpResponse;

public class HttpResponseImpl implements HttpResponse {
    private final int code;
    private final byte[] result;
    private final Map<String, List<String>> headers;

    public HttpResponseImpl(HttpURLConnection conn) throws IOException {
        code = conn.getResponseCode();
        result = read(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
        headers = new HashMap<>();
        conn.getHeaderFields().forEach((s, strings) -> headers.put(s, new ArrayList<>(strings)));
    }

    @Override
    public byte[] getResponseBody() {
        return result;
    }

    @Override
    public int getResponseCode() {
        return code;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return headers;
    }

    private byte[] read(InputStream inputStream) throws IOException {
        int len;
        byte[] bytes = new byte[1024 * 8];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while ((len = inputStream.read(bytes)) != -1) {
            os.write(bytes, 0, len);
        }
        return os.toByteArray();
    }
}
