package qingzhou.http.impl;

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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import qingzhou.http.HttpClient;
import qingzhou.http.HttpMethod;
import qingzhou.http.HttpResponse;

public class HttpClientImpl implements HttpClient {
    @Override
    public HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> params) throws Exception {
        return request(url, httpMethod, params, null);
    }

    @Override
    public HttpResponse request(String url, HttpMethod httpMethod, Map<String, String> params, Map<String, String> headers) throws Exception {
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
    public HttpResponse request(String url, HttpMethod httpMethod, byte[] body, Map<String, String> headers) throws Exception {
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
}
