package qingzhou.http.client.impl;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;

class ConnectionFactory {
    private static final ConnectionFactory instance = new ConnectionFactory();

    static ConnectionFactory getInstance() {
        return instance;
    }

    // 未指定受信证书时使用：信任所有证书且不校验主机名，保持既有行为
    private volatile SSLSocketFactory trustAllFactory;

    // 受信证书对应的 SSL 通信按证书集合缓存，避免相同信任策略重复构建、影响 TLS 会话复用
    private final Map<List<X509Certificate>, SSLSocketFactory> trustFactoryCache = new ConcurrentHashMap<>();

    HttpURLConnection getConnection(String url, int connectTimeout, int readTimeout, X509Certificate[] trustedCertificates) throws Exception {
        if (url == null || url.trim().isEmpty()) throw new IllegalArgumentException("url is missing");

        HttpURLConnection conn;
        URL http = new URL(url);
        if (url.startsWith("https:")) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) http.openConnection();
            if (trustedCertificates == null || trustedCertificates.length == 0) {
                httpsConn.setSSLSocketFactory(getTrustAllFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            } else {
                httpsConn.setSSLSocketFactory(getTrustFactory(trustedCertificates));
            }
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) http.openConnection();
        }

        setDefaultConfig(conn);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        return conn;
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
        List<X509Certificate> cacheKey = Arrays.asList(trustedCertificates.clone()); // 防御拷贝：缓存键不应随调用方持有的数组变动
        SSLSocketFactory factory = trustFactoryCache.get(cacheKey);
        if (factory != null) {
            return factory;
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        for (int i = 0; i < trustedCertificates.length; i++) {
            keyStore.setCertificateEntry("trusted" + i, trustedCertificates[i]);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        factory = newSocketFactory(trustManagerFactory.getTrustManagers());
        trustFactoryCache.put(cacheKey, factory);
        return factory;
    }

    private static SSLSocketFactory newSocketFactory(TrustManager... trustManagers) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
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
