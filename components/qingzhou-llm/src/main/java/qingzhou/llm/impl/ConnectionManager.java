package qingzhou.llm.impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

public class ConnectionManager {
    private static final SSLSocketFactory SSL_SOCKET_FACTORY;

    static {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[0], new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            SSL_SOCKET_FACTORY = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    private final String urlStr;
    private final String bearerKey;

    public ConnectionManager(String urlStr, String bearerKey) {
        this.urlStr = urlStr;
        this.bearerKey = bearerKey;
    }

    public HttpURLConnection getConnection() throws Exception {
        return createConnection();
    }

    private HttpURLConnection createConnection() throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn;
        if (urlStr.startsWith("https:")) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            httpsConn.setSSLSocketFactory(SSL_SOCKET_FACTORY);
            httpsConn.setHostnameVerifier((hostname, session) -> true);
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(10 * 60 * 1000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + bearerKey);
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setInstanceFollowRedirects(false);

        return conn;
    }
}
