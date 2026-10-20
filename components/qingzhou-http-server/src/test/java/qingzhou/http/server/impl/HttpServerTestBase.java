package qingzhou.http.server.impl;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import qingzhou.http.client.HttpClient;

/**
 * HTTP 服务测试基类：统一客户端创建与服务停止，用例只声明自身需要的启动配置。
 */
abstract class HttpServerTestBase {
    protected TestServerSupport.TestServer testServer;
    protected HttpClient client;

    @BeforeMethod
    public void createClient() {
        client = HttpClientServerIntegrationTest.buildHttpClientImpl();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() {
        if (testServer != null) {
            testServer.server.stop();
            testServer = null;
        }
    }

    protected void startServer() throws Exception {
        testServer = TestServerSupport.startServer();
    }

    protected void startServer(Map<String, String> config) throws Exception {
        testServer = TestServerSupport.startServer(config);
    }
}
