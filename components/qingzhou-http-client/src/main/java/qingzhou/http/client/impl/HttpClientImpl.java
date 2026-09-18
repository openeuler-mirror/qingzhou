package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

@Component
public class HttpClientImpl implements HttpClient {
    private ExecutorService executor;

    @Activate
    public void activate() {
        executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "qz-http-client-" + seq.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Deactivate
    public void deactivate() {
        executor.shutdownNow();
    }

    @Override
    public Response send(Request request) throws Exception {
        return send(request, null);
    }

    @Override
    public Response send(Request request, ResponseListener listener) throws Exception {
        RequestImpl req = (RequestImpl) request;
        HttpURLConnection conn = ConnectionFactory.getInstance().getConnection(req.url, req.connectTimeout, req.readTimeout, req.trustedCertificates, req.trustAll);

        if (req.method != null) {
            conn.setRequestMethod(req.method.name());
        }

        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        byte[] body = null;
        if (req.body != null) {
            body = req.body;
        } else if (req.params != null && req.files == null) {
            body = encodeForm(req.params);
            if (conn.getRequestProperty("Content-Type") == null) {
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
        }

        boolean doDisconnect = true;
        try {
            if (body != null) {
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body, 0, body.length);
                    out.flush();
                }
            } else if (req.files != null) {
                writeMultipart(req, conn);
            } else {
                conn.connect();
            }

            ResponseImpl response = new ResponseImpl(conn, listener, req.maxBodySize, executor);
            doDisconnect = false; // 连接交由 ResponseImpl 管理：正常读完可复用，取消时强制断开
            return response;
        } finally {
            if (doDisconnect) {
                conn.disconnect(); // 请求构造失败时兜底断开
            }
        }
    }

    @Override
    public Request newRequest(String url) {
        return new RequestImpl(url);
    }

    private static byte[] encodeForm(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null) continue;
            if (body.length() > 0) body.append('&');
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                    .append(URLEncoder.encode(value, "UTF-8"));
        }
        return body.length() > 0 ? body.toString().getBytes(StandardCharsets.UTF_8) : null;
    }

    private void writeMultipart(RequestImpl req, HttpURLConnection conn) throws IOException {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream()) {
            if (req.params != null) {
                for (Map.Entry<String, String> entry : req.params.entrySet()) {
                    writeTextPart(output, boundary, entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, List<String>> entry : req.files.entrySet()) {
                for (String path : entry.getValue()) {
                    writeFilePart(output, boundary, entry.getKey(), path);
                }
            }
            write(output, "--" + boundary + "--\r\n");
            output.flush();
        }
    }

    private static void writeTextPart(OutputStream output, String boundary, String name, String value) throws IOException {
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"" + safeToken(name) + "\"\r\n\r\n");
        write(output, value == null ? "" : value);
        write(output, "\r\n");
    }

    private static void writeFilePart(OutputStream output, String boundary, String name, String path) throws IOException {
        File file = new File(path);
        if (!file.isFile()) throw new FileNotFoundException(path);
        String fileName = safeToken(file.getName());

        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"" + safeToken(name) + "\"; filename=\"" + fileName + "\"\r\n");
        write(output, "Content-Type: " + guessContentType(fileName) + "\r\n\r\n");

        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[1024 * 8];
            for (int len; (len = input.read(buffer)) != -1; ) {
                output.write(buffer, 0, len);
            }
        }
        write(output, "\r\n");
    }

    private static String safeToken(String value) {
        if (value == null || value.isEmpty() || !value.matches("[^\\r\\n\"]*")) {
            throw new IllegalArgumentException("illegal multipart field: " + value);
        }
        return value;
    }

    private static void write(OutputStream output, String text) throws IOException {
        output.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String guessContentType(String fileName) {
        String type = URLConnection.guessContentTypeFromName(fileName);
        return type != null ? type : "application/octet-stream";
    }
}
