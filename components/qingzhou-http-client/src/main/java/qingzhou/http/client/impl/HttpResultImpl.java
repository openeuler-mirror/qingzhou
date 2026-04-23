package qingzhou.http.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.http.client.HttpResult;

class HttpResultImpl implements HttpResult {
    private final int code;
    private final byte[] result;
    private final Map<String, List<String>> headers;

    HttpResultImpl(HttpURLConnection conn) throws IOException {
        code = conn.getResponseCode();
        result = read(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
        headers = new HashMap<>();
        conn.getHeaderFields().forEach((s, strings) -> headers.put(s, new ArrayList<>(strings)));
    }

    @Override
    public byte[] getBody() {
        return result;
    }

    @Override
    public int getStatus() {
        return code;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
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
