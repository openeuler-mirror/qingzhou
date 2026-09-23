package qingzhou.http.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.impl.LoggerImpl;
import reactor.netty.DisposableServer;

public class HttpServerImplTest {
    private static final String KEYSTORE_PASSWORD = "qingzhou-test";

    @BeforeClass
    public void init() {
        System.setProperty("qingzhou.instance", new File("/tmp").getAbsolutePath());
        System.setProperty("qingzhou.version", "1.0");
    }

    @Test
    public void normal_start_listenHttpService() throws Exception {
        HttpServerImpl httpServer = build(0);
        HttpClientImpl httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + actualPort(httpServer)).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normal_stop_requestGetConnectException() throws Exception {
        HttpServerImpl httpServer = build(0);
        int port = actualPort(httpServer);
        httpServer.stop();
        try {
            HttpClientImpl httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            httpClient.send(httpClient.newRequest("http://localhost:" + port).method(HttpMethod.GET));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConnectException);
        }
    }

    @Test
    public void normalPath_registerHttpHandler_usePathHttpService() throws Exception {
        HttpServerImpl httpServer = build(0);
        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandlerNoAuth(httpHandler, path);

        HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + actualPort(httpServer) + path).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 200);
        Assert.assertTrue(new String(result.getBody(), StandardCharsets.UTF_8).contains(path));

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normalPath_unregisterHttpHandler_noPathHttpService() throws Exception {
        HttpServerImpl httpServer = build(0);

        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandlerNoAuth(httpHandler, path);
        httpServer.unregisterHttpHandler(httpHandler);
        HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + actualPort(httpServer) + path).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void sslEnabled_httpsRequest_returns200() throws Exception {
        HttpServerImpl httpServer = start(sslConfig(0));
        try {
            String path = "/sslTest";
            httpServer.registerHttpHandlerNoAuth((httpRequest, httpResponse) -> httpResponse.sendFinish("ssl-ok"), path);

            HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = httpClient.send(httpClient.newRequest("https://localhost:" + actualPort(httpServer) + path)
                    .trustAllCertificates()
                    .method(HttpMethod.GET));
            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "ssl-ok");
        } finally {
            httpServer.stop(); // 清理资源
        }
    }

    @Test
    public void sslEnabledWithoutKeystorePath_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig(0);
        config.remove("ssl_keystore_path");
        assertStartFails(config, "ssl_keystore_path");
    }

    @Test
    public void sslKeystorePathNotExist_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig(0);
        config.put("ssl_keystore_path", new File(System.getProperty("java.io.tmpdir"),
                "qingzhou-not-exists-" + System.nanoTime() + ".p12").getAbsolutePath());
        assertStartFails(config, "does not exist");
    }

    @Test
    public void sslKeystoreWrongPassword_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig(0);
        config.put("ssl_keystore_password", Cipher.PLAIN_PREFIX_MARKER + "wrong-password");
        try {
            start(config);
            Assert.fail("start should throw when ssl keystore password is wrong");
        } catch (IllegalStateException e) {
            Assert.assertNotNull(e.getCause()); // 由底层密钥库解析失败引起
        }
    }

    @Test
    public void startFailed_wrongKeystorePassword_resourcesReleased() throws Exception {
        Map<String, String> config = sslConfig(0);
        config.put("ssl_keystore_password", Cipher.PLAIN_PREFIX_MARKER + "wrong-password");
        HttpServerImpl httpServer = buildHttpServer(config);
        try {
            httpServer.start(config);
            Assert.fail("start should throw when ssl keystore password is wrong");
        } catch (IllegalStateException e) {
            // 激活失败不会触发 @Deactivate，泄漏的 EventLoop 线程池将永不回收
            assertFieldNull(httpServer, "loopResources");
            assertFieldNull(httpServer, "disposableServer");
        }
    }

    @Test
    public void sslInvalidKeystoreType_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig(0);
        config.put("ssl_keystore_type", "DSA");
        assertStartFails(config, "ssl_keystore_type");
    }

    static HttpServerImpl build(int port) throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", port + "");
        config.put("ssl_enabled", "false");
        return start(config);
    }

    static HttpServerImpl start(Map<String, String> config) throws Exception {
        HttpServerImpl httpServer = buildHttpServer(config);
        httpServer.start(config);
        return httpServer;
    }

    static HttpServerImpl buildHttpServer(Map<String, String> config) throws Exception {
        HttpServerImpl httpServer = new HttpServerImpl();
        LoggerImpl logger = new LoggerImpl();
        CryptoImpl crypto = new CryptoImpl();
        HandlerManager handlerManager = new HandlerManager();
        DispatcherHandler dispatcherHandler = new DispatcherHandler();
        AuthManager authManager = new AuthManager();

        setField(httpServer, "crypto", crypto);
        setField(httpServer, "logger", logger);
        setField(httpServer, "handlerManager", handlerManager);
        setField(httpServer, "dispatcherHandler", dispatcherHandler);

        setField(handlerManager, "logger", logger);

        setField(dispatcherHandler, "logger", logger);
        setField(dispatcherHandler, "authManager", authManager);
        setField(dispatcherHandler, "handlerManager", handlerManager);

        setField(authManager, "logger", logger);
        setField(authManager, "handlerManager", handlerManager);

        dispatcherHandler.init(config);
        authManager.init(config); // 生产由 OSGi @Activate 触发，手动装配时须补齐

        return httpServer;
    }

    static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Map<String, String> sslConfig(int port) {
        Map<String, String> config = new HashMap<>();
        config.put("port", port + "");
        config.put("ssl_enabled", "true");
        config.put("ssl_keystore_path", keystoreFile().getAbsolutePath());
        config.put("ssl_keystore_password", Cipher.PLAIN_PREFIX_MARKER + KEYSTORE_PASSWORD);
        config.put("ssl_keystore_type", "PKCS12");
        return config;
    }

    private static void assertStartFails(Map<String, String> config, String expectedMessage) throws Exception {
        try {
            start(config);
            Assert.fail("start should throw, expected message contains: " + expectedMessage);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessage), e.getMessage());
        }
    }

    private static void assertFieldNull(HttpServerImpl httpServer, String fieldName) throws Exception {
        Field field = HttpServerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Assert.assertNull(field.get(httpServer), fieldName + " must be released when start fails");
    }

    private static int actualPort(HttpServerImpl httpServer) throws Exception {
        Field field = HttpServerImpl.class.getDeclaredField("disposableServer");
        field.setAccessible(true);
        DisposableServer disposableServer = (DisposableServer) field.get(httpServer);
        return ((InetSocketAddress) disposableServer.address()).getPort();
    }

    private static File keystoreFile() {
        URL resource = HttpServerImplTest.class.getClassLoader().getResource("test-keystore.p12");
        Assert.assertNotNull(resource, "test-keystore.p12 is missing on the test classpath");
        return new File(resource.getFile());
    }
}
