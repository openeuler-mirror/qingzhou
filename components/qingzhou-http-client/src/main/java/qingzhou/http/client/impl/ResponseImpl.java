package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

class ResponseImpl implements Response {
    private final int code;
    private final int maxBodySize;
    // 仅流式保留：cancel() 需断开底层连接；同步响应读完即终结，无待释放资源
    private final HttpURLConnection conn;
    private final boolean streaming;

    private byte[] result;
    // cancel() 由调用线程写、流式读取线程读，volatile 保证取消状态及时可见
    private volatile boolean cancelled;

    ResponseImpl(HttpURLConnection conn, ResponseListener listener, int maxBodySize, Executor executor) throws IOException {
        this.maxBodySize = maxBodySize;
        this.conn = conn;
        this.code = conn.getResponseCode();

        if (listener != null && code >= 200 && code < 300) {
            // 2xx + 流式：后台线程逐行回调，实现打字机效果
            streaming = true;
            executor.execute(() -> readStreaming(listener));
        } else {
            // 同步模式或非 2xx：一次性读完整响应体，不 disconnect 以复用 keep-alive 连接
            streaming = false;
            result = read(responseStream());
        }
    }

    private InputStream responseStream() throws IOException {
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        return stream != null ? stream : new ByteArrayInputStream(new byte[0]);
    }

    private void readStreaming(ResponseListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream(), StandardCharsets.UTF_8))) {
            long received = 0;
            for (String line; !cancelled && (line = reader.readLine()) != null; ) {
                received += line.getBytes(StandardCharsets.UTF_8).length + 1; // 按字节计数（readLine 丢弃换行符，补回 1）：多字节字符下字符数会低估实际内存
                // 流式路径同样受 maxBodySize 约束：否则服务端一条超长行即可让整行驻留内存
                if (maxBodySize > 0 && received > maxBodySize) {
                    throw new IOException("streaming response exceeds maxBodySize: " + maxBodySize);
                }
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
            // 最终，不必检测状态，直接关闭长连接？
            conn.disconnect();
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        if (streaming) {
            conn.disconnect(); // 仅流式中止需断开连接
        }
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

    @Override
    public String getHeader(String name) {
        return conn.getHeaderField(name);
    }

    private byte[] read(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            int len;
            byte[] bytes = new byte[1024 * 8];
            while ((len = in.read(bytes)) != -1) {
                if (maxBodySize > 0 && os.size() + len > maxBodySize) {
                    throw new IOException("Response body exceeds maxBodySize: " + maxBodySize);
                }
                os.write(bytes, 0, len);
            }
            return os.toByteArray();
        }
    }
}
