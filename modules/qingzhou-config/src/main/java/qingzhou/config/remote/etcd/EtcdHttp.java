package qingzhou.config.remote.etcd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * etcd REST 需要的最小 POST 实现：只发 JSON、带可选令牌，用 JDK 自带 HttpURLConnection，
 * 避免为配置拉取引入 HTTP 组件依赖。
 */
final class EtcdHttp {
    static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private final int connectTimeout;
    private final int readTimeout;

    EtcdHttp(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    Response post(String url, String token, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            if (token != null) connection.setRequestProperty("Authorization", token);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            // 4xx/5xx 的响应体在 error stream，读取后随异常输出 etcd 的具体原因
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Response(status, in == null ? "" : read(in));
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024 * 4];
            for (int n; (n = input.read(buffer)) != -1; ) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
