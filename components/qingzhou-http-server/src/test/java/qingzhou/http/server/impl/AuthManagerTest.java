package qingzhou.http.server.impl;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.auth.AuthResult;
import qingzhou.http.server.auth.impl.AuthManager;
import qingzhou.http.server.impl.TestServerSupport.TestServer;

/**
 * 认证链路自动化测试集：免认证标记的作用域、认证器对响应的写入。
 * <p>
 * 默认未注册系统级 Authenticator，未显式注入认证器时需认证的路径返回 401。
 */
public class AuthManagerTest {

    @Test
    public void sameHandlerInstanceAtTwoPaths_authPath_stillRequiresAuth() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            HttpHandler handler = (request, response) -> response.send("leak").finish();
            testServer.server.registerHttpHandler(handler, "/noAuthPath", true);
            testServer.server.registerHttpHandler(handler, "/authPath"); // 同一实例，认证要求不得被串味

            Assert.assertEquals(statusOf(testServer, "/noAuthPath"), 200);
            Assert.assertEquals(statusOf(testServer, "/authPath"), 401);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void authenticator_writesRenewalHeader_headerReachesClient() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", "0");
        config.put("ssl_enabled", "false");
        AuthManager authManager = new AuthManager();
        HttpServerImpl server = HttpServerImplTest.start(config, authManager);
        try {
            // 认证闸门早于 body 处理：滑动续期在此写 Cookie，响应对象必须已装配
            authManager.addAuthenticator((request, response) -> {
                response.header("X-Renew-Token", "renewed");
                return AuthResult.pass("user", new String[0]);
            });
            server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/authPath");

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + server.getPort() + "/authPath")
                    .method(HttpMethod.GET));
            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(result.getHeader("X-Renew-Token"), "renewed");
        } finally {
            server.stop();
        }
    }

    private static int statusOf(TestServer testServer, String path) throws Exception {
        HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
        Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + path)
                .method(HttpMethod.GET));
        return result.getStatus();
    }
}
