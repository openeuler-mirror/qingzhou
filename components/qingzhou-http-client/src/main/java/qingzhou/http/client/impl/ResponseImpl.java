package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

class ResponseImpl implements Response {
    private static final Executor executor = Executors.newFixedThreadPool(7);

    private final int code;
    private byte[] result;

    ResponseImpl(HttpURLConnection conn, ResponseListener listener) throws IOException {
        code = conn.getResponseCode();

        InputStream responseStream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
        if (listener == null) {
            result = read(responseStream);
        } else {
            executor.execute(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        listener.onBody(line);
                    }
                    listener.onComplete();
                } catch (Throwable t) {
                    listener.onError(t);
                } finally {
                    conn.disconnect();
                }
            });
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
