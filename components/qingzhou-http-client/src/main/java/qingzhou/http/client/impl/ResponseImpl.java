package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

class ResponseImpl implements Response {
    private static volatile ExecutorService executor;

    private static ExecutorService executor() {
        ExecutorService e = executor;
        if (e == null || e.isShutdown()) {
            synchronized (ResponseImpl.class) {
                e = executor;
                if (e == null || e.isShutdown()) {
                    e = Executors.newCachedThreadPool(new ThreadFactory() {
                        private final AtomicInteger seq = new AtomicInteger();

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r, "qz-http-client-" + seq.incrementAndGet());
                            thread.setDaemon(true);
                            return thread;
                        }
                    });
                    executor = e;
                }
            }
        }
        return e;
    }

    static void shutdown() {
        ExecutorService e = executor;
        executor = null;
        if (e != null) {
            e.shutdownNow();
        }
    }

    private final int code;
    private final HttpURLConnection conn;
    private final boolean streaming;

    private volatile byte[] result;
    private volatile boolean cancelled;

    ResponseImpl(HttpURLConnection conn, ResponseListener listener) throws IOException {
        this.conn = conn;
        this.code = conn.getResponseCode();

        if (listener != null && code >= 200 && code < 300) {
            // 2xx + 流式：后台线程逐行回调，实现打字机效果
            streaming = true;
            executor().execute(() -> readStreaming(listener));
        } else {
            // 同步模式或非 2xx：一次性读完整响应体，交由调用方根据 getStatus() 处理
            streaming = false;
            result = read(responseStream(code));
        }
    }

    private InputStream responseStream(int code) throws IOException {
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        return stream != null ? stream : new ByteArrayInputStream(new byte[0]);
    }

    private void readStreaming(ResponseListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream(code), StandardCharsets.UTF_8))) {
            for (String line; !cancelled && (line = reader.readLine()) != null; ) {
                listener.onBody(line);
            }
            if (!cancelled) {
                listener.onComplete();
            }
        } catch (Throwable t) {
            if (!cancelled) {
                listener.onError(t);
            }
        } finally {
            if (cancelled) {
                conn.disconnect(); // 被取消时强制断开底层连接
            }
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        conn.disconnect();
    }

    @Override
    public byte[] getBody() {
        if (streaming) {
            throw new IllegalStateException("Response body is not available in streaming mode");
        }

        return result;
    }

    @Override
    public int getStatus() {
        return code;
    }

    private byte[] read(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            int len;
            byte[] bytes = new byte[1024 * 8];
            while ((len = in.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            return os.toByteArray();
        }
    }
}
