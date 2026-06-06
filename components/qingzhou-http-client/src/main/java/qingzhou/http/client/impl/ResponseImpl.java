package qingzhou.http.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import qingzhou.http.client.Response;

class ResponseImpl implements Response {
    private final int code;
    private byte[] result;

    ResponseImpl(HttpURLConnection conn) throws IOException {
        code = conn.getResponseCode();
        try {
            result = read(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
        } catch (Exception ignored) {
        }
    }

    @Override
    public byte[] getBody() {
        return result;
    }

    @Override
    public int getStatus() {
        return code;
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
