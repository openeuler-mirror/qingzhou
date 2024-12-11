package qingzhou.http.impl;

import qingzhou.http.HttpClient;
import qingzhou.http.HttpResponse;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.OutputStream;
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

public class HttpClientImpl implements HttpClient {
    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws Exception {
        return send(url, null, headers, "GET");
    }

    @Override
    public HttpResponse post(String url, Map<String, String> params) throws Exception {
        return post(url, params, null);
    }

    @Override
    public HttpResponse post(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
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
        return post(url, bodyStr.toString().getBytes(StandardCharsets.UTF_8), headers);
    }

    @Override
    public HttpResponse post(String url, byte[] body, Map<String, String> headers) throws Exception {
        return send(url, body, headers, "POST");
    }

    @Override
    public HttpResponse delete(String url, Map<String, String> headers) throws Exception {
        return send(url, null, headers, "DELETE");
    }

    @Override
    public HttpResponse put(String url, byte[] body, Map<String, String> headers) throws Exception {
        return send(url, body, headers, "PUT");
    }

    private HttpResponse send(String url, byte[] body, Map<String, String> headers, String method) throws Exception {
        HttpURLConnection conn = buildConnection(url);
        setDefaultHttpURLConnection(conn);

        if (method != null) {
            conn.setRequestMethod(method);
        }

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
            return new HttpResponseImpl(conn);
        } finally {
            conn.disconnect();
        }
    }

    private void setDefaultHttpURLConnection(HttpURLConnection conn) throws ProtocolException {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("accept", "*/*");
        conn.setInstanceFollowRedirects(false);
    }

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
}
