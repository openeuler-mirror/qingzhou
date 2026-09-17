package qingzhou.http.client.impl;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

class ConnectionFactory {
    private static final ConnectionFactory instance = new ConnectionFactory();

    static ConnectionFactory getInstance() {
        return instance;
    }

    // 未显式调用 trustAllCertificates 时使用：信任所有证书且不校验主机名
    private volatile SSLSocketFactory trustAllFactory;

    HttpURLConnection getConnection(String url, int connectTimeout, int readTimeout, X509Certificate[] trustedCertificates, boolean trustAll) throws Exception {
        if (url == null || url.trim().isEmpty()) throw new IllegalArgumentException("url is missing");

        URL http = new URL(url);
        String protocol = http.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new IllegalArgumentException("unsupported protocol: " + protocol);
        }

        HttpURLConnection conn = (HttpURLConnection) http.openConnection();
        if (protocol.equals("https")) {
            configSsl((HttpsURLConnection) conn, trustedCertificates, trustAll);
        }

        setDefaultConfig(conn);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        return conn;
    }

    private void configSsl(HttpsURLConnection httpsConn, X509Certificate[] trustedCertificates, boolean trustAll) throws Exception {
        if (trustAll) {
            // 信任所有证书且不校验主机名，存在中间人攻击风险
            httpsConn.setSSLSocketFactory(getTrustAllFactory());
            httpsConn.setHostnameVerifier((hostname, session) -> true);
        } else if (trustedCertificates != null && trustedCertificates.length > 0) {
            httpsConn.setSSLSocketFactory(getTrustFactory(trustedCertificates));
        }
        // 其余情况使用 JVM 默认 CA 信任库校验
    }

    private SSLSocketFactory getTrustAllFactory() throws Exception {
        if (trustAllFactory == null) {
            synchronized (this) {
                if (trustAllFactory == null) {
                    X509TrustManager trustAll = new X509TrustManager() {
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
                    trustAllFactory = newSocketFactory(trustAll);
                }
            }
        }
        return trustAllFactory;
    }

    private SSLSocketFactory getTrustFactory(X509Certificate[] trustedCertificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        for (int i = 0; i < trustedCertificates.length; i++) {
            keyStore.setCertificateEntry("trusted" + i, trustedCertificates[i]);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        return newSocketFactory(trustManagerFactory.getTrustManagers());
    }

    private SSLSocketFactory newSocketFactory(TrustManager... trustManagers) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private void setDefaultConfig(HttpURLConnection conn) throws ProtocolException {
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("accept", "*/*");
        conn.setInstanceFollowRedirects(false);
        // 不强制 Connection: close，交由 JDK 默认 keep-alive 复用连接
    }
}
