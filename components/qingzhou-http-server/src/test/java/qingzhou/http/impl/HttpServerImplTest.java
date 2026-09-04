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
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.impl.LoggerImpl;
import reactor.netty.DisposableServer;

public class HttpServerImplTest {
    private static final String KEYSTORE_PASSWORD = "qingzhou-test";

    @Test
    public void normal_start_listenHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        HttpClientImpl httpClient = new HttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + port).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normal_stop_requestGetConnectException() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        httpServer.stop();
        try {
            HttpClientImpl httpClient = new HttpClientImpl();
            httpClient.send(httpClient.newRequest("http://localhost:" + port).method(HttpMethod.GET));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConnectException);
        }
    }

    @Test
    public void normalPath_registerHttpHandler_usePathHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandlerNoAuth(httpHandler, path);

        HttpClient httpClient = new HttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + port + path).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 200);
        Assert.assertTrue(new String(result.getBody()).contains(path));

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normalPath_unregisterHttpHandler_noPathHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);

        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandlerNoAuth(httpHandler, path);
        httpServer.unregisterHttpHandler(httpHandler);
        HttpClient httpClient = new HttpClientImpl();
        Response result = httpClient.send(httpClient.newRequest("http://localhost:" + port + path).method(HttpMethod.GET));
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void sslEnabled_httpsRequest_returns200() throws Exception {
        HttpServerImpl httpServer = start(sslConfig(0));
        try {
            String path = "/sslTest";
            httpServer.registerHttpHandlerNoAuth((httpRequest, httpResponse) -> httpResponse.sendFinish("ssl-ok"), path);

            HttpClient httpClient = new HttpClientImpl();
            Response result = httpClient.send(httpClient.newRequest("https://localhost:" + actualPort(httpServer) + path).method(HttpMethod.GET));
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
        config.put("ssl_keystore_password", "wrong-password");
        try {
            start(config);
            Assert.fail("start should throw when ssl keystore password is wrong");
        } catch (IllegalStateException e) {
            Assert.assertNotNull(e.getCause()); // 由底层密钥库解析失败引起
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
        return start(config);
    }

    static HttpServerImpl start(Map<String, String> config) throws Exception {
        HttpServerImpl httpServer = new HttpServerImpl();
        Field loggerField = HttpServerImpl.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(httpServer, new LoggerImpl());

        httpServer.start(config);
        return httpServer;
    }

    private static Map<String, String> sslConfig(int port) {
        Map<String, String> config = new HashMap<>();
        config.put("port", port + "");
        config.put("ssl_enabled", "true");
        config.put("ssl_keystore_path", keystoreFile().getAbsolutePath());
        config.put("ssl_keystore_password", KEYSTORE_PASSWORD);
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
