package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import qingzhou.http.client.*;

@Component(configurationPid = "qingzhou-http-client", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HttpClientImpl implements HttpClient {
    // 并发流式响应线程上限：超出后直接拒绝（快速失败），避免线程无限增长
    private static final int MAX_STREAM_THREADS = 200;

    private boolean allowSslTrustAll;
    private ExecutorService executor;

    @Activate
    public void init(Map<String, String> config) {
        // 与 Configs 的严格布尔策略一致：非 true/false 一律报错，避免拼写/大小写误配导致静默失效
        String trustAll = config.get("allow_ssl_trust_all");
        if (trustAll == null || trustAll.trim().isEmpty() || "false".equals(trustAll.trim())) {
            allowSslTrustAll = false;
        } else if ("true".equals(trustAll.trim())) {
            allowSslTrustAll = true;
        } else {
            throw new IllegalArgumentException("qingzhou-http-client.allow_ssl_trust_all must be 'true' or 'false', got: " + trustAll);
        }

        // 有界线程池：每个流式响应独占一个线程直至读毕，无上限会在海量并发流式响应时耗尽线程资源
        executor = new ThreadPoolExecutor(0, MAX_STREAM_THREADS, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "qz-http-client-" + seq.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        }, new ThreadPoolExecutor.AbortPolicy());
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
    public Response send(Request request, ClientListener listener) throws Exception {
        if (!(request instanceof RequestImpl)) { // 明确报错：强转失败只暴露 ClassCastException，掩盖真实原因（非本实现构建的请求）
            throw new IllegalArgumentException("unsupported request type: "
                    + (request == null ? "null" : request.getClass().getName()));
        }
        RequestImpl req = (RequestImpl) request;
        // GET/HEAD 不允许请求体：参数须拼到查询串，否则 HttpURLConnection 会因写 body 抛 ProtocolException。
        // 拼接结果只用于本次发送，不回写请求对象：同一 Request 可重复 send，且不在线程间共享可变状态
        boolean methodHasBody = req.method != null && req.method != HttpMethod.GET && req.method != HttpMethod.HEAD;
        if (!methodHasBody && (req.body != null || req.files != null)) {
            // 显式拒绝：静默走 getOutputStream()/writeMultipart 会在运行期抛 ProtocolException，错误点远离真实原因
            throw new IllegalStateException(req.method + " does not allow a request body or upload files, put parameters into the query string");
        }
        String requestUrl = req.url;
        if (!methodHasBody && req.params != null) { // 上方已拒绝 body/files，非 body 方法仅剩查询串传参
            requestUrl = appendQuery(req.url, req.params);
        }

        HttpURLConnection conn = ConnectionFactory.getInstance().getConnection(requestUrl, req.connectTimeout, req.readTimeout, req.trustedCertificates, req.trustAll);

        if (req.method != null) {
            conn.setRequestMethod(req.method.name());
        }

        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        byte[] body = req.body;
        if (body == null && req.params != null && req.files == null && methodHasBody) {
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
    public Request buildRequest(String url) {
        return new RequestImpl(url, allowSslTrustAll);
    }

    private static byte[] encodeForm(Map<String, String> params) throws UnsupportedEncodingException {
        String encoded = encodePairs(params);
        return encoded.isEmpty() ? null : encoded.getBytes(StandardCharsets.UTF_8);
    }

    // 将参数拼到 URL 查询串（供 GET/HEAD 使用），与 encodeForm 复用同一套编码
    private static String appendQuery(String url, Map<String, String> params) throws UnsupportedEncodingException {
        String query = encodePairs(params);
        if (query.isEmpty()) return url;
        return url + (url.indexOf('?') < 0 ? "?" : "&") + query;
    }

    private static String encodePairs(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder pairs = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null) continue;
            if (pairs.length() > 0) pairs.append('&');
            pairs.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                    .append(URLEncoder.encode(value, "UTF-8"));
        }
        return pairs.toString();
    }

    private void writeMultipart(RequestImpl req, HttpURLConnection conn) throws IOException {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        // 启用分块流式：否则 HttpURLConnection 会把整个 multipart 请求体缓冲进堆，大文件上传直接 OOM。
        // 文件本身按 8KB 分块读取，配合此模式即可边读边发，不再整文件驻留内存
        conn.setChunkedStreamingMode(8192);

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

    // multipart 字段名/文件名合法性：禁止换行与引号（预编译，多文件上传时免重复编译正则）
    private static final java.util.regex.Pattern MULTIPART_TOKEN = java.util.regex.Pattern.compile("[^\\r\\n\"]*");

    private static String safeToken(String value) {
        if (value == null || value.isEmpty() || !MULTIPART_TOKEN.matcher(value).matches()) {
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
