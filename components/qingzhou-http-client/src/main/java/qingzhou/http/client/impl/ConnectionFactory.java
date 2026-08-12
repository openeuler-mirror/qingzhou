package qingzhou.http.client.impl;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

class ConnectionFactory {
    private static final int DEFAULT_CONNECT_TIMEOUT = 60 * 1000;
    private static final int DEFAULT_READ_TIMEOUT = 10 * 60 * 1000;

    private static final ConnectionFactory instance = new ConnectionFactory();

    static ConnectionFactory getInstance() {
        return instance;
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

    HttpURLConnection getConnection(String url, int connectTimeout, int readTimeout) throws Exception {
        if (url == null || url.trim().isEmpty()) throw new IllegalArgumentException("url is missing");

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

        setDefaultConfig(conn);
        conn.setConnectTimeout(connectTimeout > 0 ? connectTimeout : DEFAULT_CONNECT_TIMEOUT);
        conn.setReadTimeout(readTimeout > 0 ? readTimeout : DEFAULT_READ_TIMEOUT);

        return conn;
    }

    private void setDefaultConfig(HttpURLConnection conn) throws ProtocolException {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("accept", "*/*");
        conn.setInstanceFollowRedirects(false);
        // 不强制 Connection: close，交由 JDK 默认 keep-alive 复用连接
    }
}
