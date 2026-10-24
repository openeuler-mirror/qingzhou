package qingzhou.http.server.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.osgi.framework.Constants;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.auth.impl.AuthManager;
import qingzhou.logger.impl.LoggerImpl;

import java.io.File;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HttpServerImplTest {
    private static final String KEYSTORE_PASSWORD = "qingzhou-test";

    @Test
    public void normal_start_listenHttpService() throws Exception {
        HttpServerImpl httpServer = build();
        try {
            HttpClientImpl httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = httpClient.send(httpClient.buildRequest("http://localhost:" + httpServer.getPort()).method(HttpMethod.GET));
            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            httpServer.stop(); // 断言失败也不能泄漏 Netty 线程与监听端口
        }
    }

    @Test
    public void normal_stop_requestGetConnectException() throws Exception {
        HttpServerImpl httpServer = build();
        int port = httpServer.getPort();
        httpServer.stop();
        try {
            HttpClientImpl httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            httpClient.send(httpClient.buildRequest("http://localhost:" + port).method(HttpMethod.GET));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConnectException);
        }
    }

    @Test
    public void normalPath_registerHttpHandler_usePathHttpService() throws Exception {
        HttpServerImpl httpServer = build();
        try {
            String path = "/testHttp";
            HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.send("Hello: " + httpRequest.getPath()).finish();
            httpServer.registerHttpHandler(httpHandler, path, true);

            HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = httpClient.send(httpClient.buildRequest("http://localhost:" + httpServer.getPort() + path).method(HttpMethod.GET));
            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertTrue(new String(result.getBody(), StandardCharsets.UTF_8).contains(path));
        } finally {
            httpServer.stop(); // 断言失败也不能泄漏 Netty 线程与监听端口
        }
    }

    @Test
    public void normalPath_unregisterHttpHandler_noPathHttpService() throws Exception {
        HttpServerImpl httpServer = build();
        try {
            String path = "/testHttp";
            HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.send("Hello: " + httpRequest.getPath()).finish();
            httpServer.registerHttpHandler(httpHandler, path, true);
            httpServer.unregisterHttpHandler(httpHandler);
            HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = httpClient.send(httpClient.buildRequest("http://localhost:" + httpServer.getPort() + path).method(HttpMethod.GET));
            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            httpServer.stop(); // 断言失败也不能泄漏 Netty 线程与监听端口
        }
    }

    @Test
    public void sslEnabled_httpsRequest_returns200() throws Exception {
        HttpServerImpl httpServer = start(sslConfig());
        try {
            String path = "/sslTest";
            httpServer.registerHttpHandler((httpRequest, httpResponse) -> httpResponse.send("ssl-ok").finish(), path, true);

            HttpClient httpClient = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = httpClient.send(httpClient.buildRequest("https://localhost:" + httpServer.getPort() + path)
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
        Map<String, String> config = sslConfig();
        config.remove("ssl_keystore_path");
        assertStartFails(config, "ssl_keystore_path");
    }

    @Test
    public void sslKeystorePathNotExist_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig();
        config.put("ssl_keystore_path", new File(System.getProperty("java.io.tmpdir"),
                "qingzhou-not-exists-" + System.nanoTime() + ".p12").getAbsolutePath());
        assertStartFails(config, "does not exist");
    }

    @Test
    public void sslKeystoreWrongPassword_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig();
        config.put("ssl_keystore_password", Cipher.PLAIN_PREFIX_MARKER + "wrong-password");
        try {
            start(config);
            Assert.fail("start should throw when ssl keystore password is wrong");
        } catch (IllegalStateException e) {
            Assert.assertNotNull(e.getCause()); // 由底层密钥库解析失败引起
        }
    }

    @Test
    public void sslInvalidKeystoreType_start_throwsException() throws Exception {
        Map<String, String> config = sslConfig();
        config.put("ssl_keystore_type", "DSA");
        assertStartFails(config, "ssl_keystore_type");
    }

    @Test
    public void invalidPort_start_throwsExceptionWithKeyName() throws Exception {
        Map<String, String> config = sslConfig();
        config.put("port", "not-a-number");
        // 数值解析失败须带配置项名：裸 NumberFormatException 无法定位是哪一项写错
        assertStartFails(config, "port must be an integer");
    }

    static HttpServerImpl build() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", "0");
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
        // 与 crypto.init 同模式补齐激活：日志调用依赖 init 初始化的 throttle/tinylog，缺激活则首次日志即 NPE；
        // 非默认日志器名（~test）跳过启动 banner，避免每个用例刷屏
        Map<String, Object> loggerConfig = new HashMap<>();
        loggerConfig.put(Constants.SERVICE_PID, "qingzhou-logger~test");
        logger.init(loggerConfig);
        CryptoImpl crypto = new CryptoImpl();
        crypto.init(new HashMap<String, String>() {{
            put("qingzhou.instance", new File("/tmp").getAbsolutePath());
            put("qingzhou.version", "1.0");
        }});
        HandlerManager handlerManager = new HandlerManager();
        DispatcherHandler dispatcherHandler = new DispatcherHandler();
        AuthManager authManager = new AuthManager();

        setField(httpServer, "crypto", crypto);
        setField(httpServer, "logger", logger);
        setField(httpServer, "handlerManager", handlerManager);
        setField(httpServer, "dispatcherHandler", dispatcherHandler);

        setField(handlerManager, "logger", logger);
        setField(crypto, "logger", logger);

        setField(dispatcherHandler, "logger", logger);
        setField(dispatcherHandler, "authManager", authManager);
        setField(dispatcherHandler, "handlerManager", handlerManager);

        setField(authManager, "logger", logger);

        dispatcherHandler.init(config);
        authManager.init(config); // 生产由 OSGi @Activate 触发，手动装配时须补齐

        return httpServer;
    }

    static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Map<String, String> sslConfig() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", "0");
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

    private static File keystoreFile() throws Exception {
        URL resource = HttpServerImplTest.class.getClassLoader().getResource("test-keystore.p12");
        Assert.assertNotNull(resource, "test-keystore.p12 is missing on the test classpath");
        return Paths.get(resource.toURI()).toFile(); // URL 含空格/中文路径时 getFile() 会解析失败
    }
}
