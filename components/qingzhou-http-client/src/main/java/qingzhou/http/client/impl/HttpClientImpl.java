package qingzhou.http.client.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;

@Component
public class HttpClientImpl implements HttpClient {
    @Override
    public Response send(Request request) throws Exception {
        RequestImpl req = (RequestImpl) request;
        HttpURLConnection conn = ConnectionFactory.getInstance().getConnection(req.url);

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
            StringBuilder bodyStr = new StringBuilder();
            boolean isFirst = true;
            for (Map.Entry<String, String> entry : req.params.entrySet()) {
                String value = entry.getValue();
                if (value == null) continue;
                if (!isFirst) bodyStr.append('&');
                isFirst = false;

                bodyStr.append(entry.getKey()).append('=');
                bodyStr.append(URLEncoder.encode(value, "UTF-8"));
            }
            body = bodyStr.toString().getBytes(StandardCharsets.UTF_8);
        }

        conn.connect();
        try {
            if (body != null) {
                conn.setRequestProperty("Content-Length", String.valueOf(body.length));
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body, 0, body.length);
                    out.flush();
                }
            } else if (req.files != null) {
                for (Map.Entry<String, String> entry : req.files.entrySet()) {
                    String fieldName = entry.getKey();
                    String values = entry.getValue();
                    if (fieldName == null || fieldName.isEmpty() || values == null || values.isEmpty()) {
                        throw new IllegalArgumentException(fieldName + "=" + values);
                    }
                    for (String f : values.split(",")) {
                        if (f.isEmpty() || !new File(f).isFile())
                            throw new FileNotFoundException(f);
                    }
                }
                sendFileStream(req, conn);
            }

            return new ResponseImpl(conn);
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public Request newRequest(String url) {
        return new RequestImpl(url);
    }

    private void sendFileStream(RequestImpl req, HttpURLConnection conn) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {

            // 1. 写入文本字段
            if (req.params != null) {
                for (Map.Entry<String, String> entry : req.params.entrySet()) {
                    writer.append("--").append(boundary).append("\r\n");
                    writer.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n");
                    writer.append("\r\n");
                    writer.append(entry.getValue()).append("\r\n");
                    writer.flush();
                }
            }

            // 2. 写入文件字段
            for (Map.Entry<String, String> entry : req.files.entrySet()) {
                String fieldName = entry.getKey();
                String values = entry.getValue();
                for (String f : values.split(",")) {
                    File file = new File(f);
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
                        byte[] buffer = new byte[1024 * 8];
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

            // 3. 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }
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
