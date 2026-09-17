package qingzhou.http.client.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import org.testng.Assert;
import org.testng.annotations.Test;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;

public class ConnectionFactoryTest {
    private static final String KEYSTORE_PASSWORD = "qingzhou-test";
    private static final String SERVER_KEYSTORE = "test-server.p12"; // 服务端证书：CN=localhost
    private static final String OTHER_KEYSTORE = "test-other.p12"; // 对照证书：CN=other，非服务端证书
    private static final String MATCH_HOST = "localhost"; // 与服务端证书 CN 一致的主机名

    @Test
    public void noTrustedCertificates_selfSignedServer_handshakeFails() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();
            try {
                client.send(client.newRequest(url(MATCH_HOST, port)).method(HttpMethod.GET));
                Assert.fail("未指定受信证书时应按 JVM 默认 CA 校验，自签名服务端请求应当失败");
            } catch (Exception e) {
                Assert.assertTrue(isHandshakeFailure(e), "期望握手失败，实际异常：" + e);
            }
        });
    }

    @Test
    public void trustAllCertificates_selfSignedServerWithMismatchedHost_requestSucceeds() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();
            Response response = client.send(client.newRequest(url(mismatchHost, port))
                    .method(HttpMethod.GET)
                    .trustAllCertificates());

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "ok");
        });
    }

    @Test
    public void matchedTrustedCertificate_selfSignedServer_requestSucceeds() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();
            Response response = client.send(client.newRequest(url(MATCH_HOST, port))
                    .method(HttpMethod.GET)
                    .trustedCertificates(certificate(SERVER_KEYSTORE)));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "ok");
        });
    }

    @Test
    public void unmatchedTrustedCertificate_selfSignedServer_handshakeFails() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();
            try {
                client.send(client.newRequest(url(MATCH_HOST, port))
                        .method(HttpMethod.GET)
                        .trustedCertificates(certificate(OTHER_KEYSTORE)));
                Assert.fail("服务端证书不在受信证书内，请求应当失败");
            } catch (Exception e) {
                Assert.assertTrue(isHandshakeFailure(e), "期望握手失败，实际异常：" + e);
            }
        });
    }

    @Test
    public void trustedCertificateHostnameMismatch_selfSignedServer_handshakeFails() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();
            try {
                client.send(client.newRequest(url(mismatchHost, port))
                        .method(HttpMethod.GET)
                        .trustedCertificates(certificate(SERVER_KEYSTORE)));
                Assert.fail("证书与主机名不匹配，请求应当失败");
            } catch (Exception e) {
                Assert.assertTrue(isHandshakeFailure(e), "期望主机名校验失败，实际异常：" + e);
            }
        });
    }

    @Test
    public void trustedAndTrustAllRequests_sameClient_eachRequestIsolated() throws Exception {
        withSelfSignedServer((mismatchHost, port) -> {
            HttpClient client = new HttpClientImpl();

            Response trusted = client.send(client.newRequest(url(MATCH_HOST, port))
                    .method(HttpMethod.GET)
                    .trustedCertificates(certificate(SERVER_KEYSTORE)));
            Assert.assertEquals(trusted.getStatus(), 200);

            // 上一次请求的受信证书不应影响后续请求的校验策略
            Response trustAll = client.send(client.newRequest(url(mismatchHost, port))
                    .method(HttpMethod.GET)
                    .trustAllCertificates());
            Assert.assertEquals(trustAll.getStatus(), 200);
        });
    }

    private interface ServerTest {
        void run(String mismatchHost, int port) throws Exception;
    }

    private void withSelfSignedServer(ServerTest test) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverSslContext()));
        server.createContext("/", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            test.run("127.0.0.1", server.getAddress().getPort());
        } finally {
            server.stop(0);
        }
    }

    private static String url(String host, int port) {
        return "https://" + host + ":" + port + "/";
    }

    private static SSLContext serverSslContext() throws Exception {
        KeyStore keyStore = loadKeyStore(SERVER_KEYSTORE);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private static X509Certificate certificate(String resourceName) throws Exception {
        KeyStore keyStore = loadKeyStore(resourceName);
        return (X509Certificate) keyStore.getCertificate(keyStore.aliases().nextElement());
    }

    private static KeyStore loadKeyStore(String resourceName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = ConnectionFactoryTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            Assert.assertNotNull(input, resourceName + " is missing on the test classpath");
            keyStore.load(input, KEYSTORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }

    private static boolean isHandshakeFailure(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SSLException) return true;
        }
        return false;
    }
}
