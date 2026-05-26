package qingzhou.http.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;

public class HttpClientImpl implements HttpClient {

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception {
        return request(url, httpMethod, params, null);
    }

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, Map<String, String> params,
            Map<String, String> headers) throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryBuilder.length() > 0) queryBuilder.append('&');
                queryBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                queryBuilder.append('=');
                queryBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        }
        byte[] body = queryBuilder.length() > 0 ? queryBuilder.toString().getBytes(StandardCharsets.UTF_8) : null;
        return doRequest(url, httpMethod, body, headers);
    }

    @Override
    public HttpResult request(String url, HttpMethod httpMethod, byte[] body, Map<String, String> headers)
            throws Exception {
        return doRequest(url, httpMethod, body, headers);
    }

    private HttpResult doRequest(String urlStr, HttpMethod method, byte[] body, Map<String, String> headers)
            throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.name());
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setDoInput(true);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        if (body != null && body.length > 0) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            OutputStream os = conn.getOutputStream();
            os.write(body);
            os.flush();
        }

        int status = conn.getResponseCode();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream is = conn.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        } catch (Exception e) {
            // No response body
        }

        byte[] responseBody = baos.toByteArray();
        Map<String, List<String>> responseHeaders = conn.getHeaderFields();

        conn.disconnect();

        return new HttpResultImpl(responseBody, status, responseHeaders);
    }

    private static class HttpResultImpl implements HttpResult {
        private final byte[] body;
        private final int status;
        private final Map<String, List<String>> headers;

        HttpResultImpl(byte[] body, int status, Map<String, List<String>> headers) {
            this.body = body;
            this.status = status;
            this.headers = headers;
        }

        @Override
        public byte[] getBody() {
            return body;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }
}