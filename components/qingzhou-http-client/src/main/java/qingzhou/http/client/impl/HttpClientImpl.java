package qingzhou.http.client.impl;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

@Component
public class HttpClientImpl implements HttpClient {
    @Override
    public HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception {
        return request(url, httpMethod, params, null);
    }

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params, Map<String, String> headers) throws Exception {
        StringBuilder bodyStr = new StringBuilder();
        if (params != null) {
            boolean isFirst = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String value = entry.getValue();
                if (value == null) continue;
                if (!isFirst) bodyStr.append('&');
                isFirst = false;

                bodyStr.append(entry.getKey()).append('=');
                bodyStr.append(URLEncoder.encode(value, "UTF-8"));
            }
        }
        byte[] bytes = bodyStr.toString().getBytes(StandardCharsets.UTF_8);
        return request(url, httpMethod, bytes, headers);
    }

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params, Map<String, String> headers, Map<String, String> files) throws Exception {
        HttpURLConnection conn = buildConnection(url);
        setDefaultHttpURLConnection(conn);

        if (httpMethod == null) httpMethod = HttpMethod.GET;
        conn.setRequestMethod(httpMethod.name());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {

            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
                    writer.append("\r\n");
                    writer.append(entry.getValue()).append("\r\n");
                    writer.flush();
                }
            }

            // 2. 写入文件字段
            if (files != null) {
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    String fieldName = entry.getKey();
                    String values = entry.getValue();
                    if (values == null || values.isEmpty()) {
                        continue;
                    }
                    for (String f : values.split(",")) {
                        if (f.isEmpty()) {
                            continue;
                        }
                        File file = new File(f);
                        if (!file.exists() || !file.isFile()) {
                            throw new IOException("文件不存在: " + f);
                        }

                        // 写入文件部分的头部
                        writer.append("--").append(boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"").append(fieldName)
                                .append("\"; filename=\"").append(file.getName()).append("\"\r\n");
                        // 根据文件扩展名猜测 Content-Type，默认 application/octet-stream
                        String contentType = guessContentType(file.getName());
                        writer.append("Content-Type: ").append(contentType).append("\r\n");
                        writer.append("\r\n");
                        writer.flush();

                        // 写入文件的二进制内容
                        try (FileInputStream fileInput = new FileInputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = fileInput.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                            output.flush();
                        }

                        // 文件内容结束后需要额外换行
                        writer.append("\r\n");
                        writer.flush();
                    }
                }
            }

            // 3. 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();

            return new HttpResultImpl(conn);
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, byte[] body, Map<String, String> headers) throws Exception {
        if (url == null || url.trim().isEmpty()) throw new IllegalArgumentException("url is null or empty");

        HttpURLConnection conn = buildConnection(url);
        setDefaultHttpURLConnection(conn);

        if (httpMethod == null) httpMethod = HttpMethod.GET;
        conn.setRequestMethod(httpMethod.name());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        if (body != null) {
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(body, 0, body.length);
            outputStream.flush();
            outputStream.close();
        }

        try {
            conn.connect();
            return new HttpResultImpl(conn);
        } finally {
            conn.disconnect();
        }
    }

    private void setDefaultHttpURLConnection(HttpURLConnection conn) throws ProtocolException {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(10 * 60 * 1000);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("accept", "*/*");
        conn.setInstanceFollowRedirects(false);
    }

    private SSLSocketFactory ssf;
    private final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private HttpURLConnection buildConnection(String url) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        HttpURLConnection conn;
        URL http = new URL(url);
        if (url.startsWith("https:")) {
            if (ssf == null) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(new KeyManager[0], new TrustManager[]{TRUST_ALL_MANAGER}, new SecureRandom());
                ssf = sslContext.getSocketFactory();
            }
            HttpsURLConnection httpsConn = (HttpsURLConnection) http.openConnection();
            httpsConn.setSSLSocketFactory(ssf);
            httpsConn.setHostnameVerifier((hostname, session) -> true);
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) http.openConnection();
        }

        return conn;
    }

    private static String guessContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
