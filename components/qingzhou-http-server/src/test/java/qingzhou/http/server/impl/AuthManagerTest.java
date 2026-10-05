package qingzhou.http.server.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.impl.TestServerSupport.TestServer;

/**
 * 认证链路自动化测试集：免认证标记的作用域、自定义认证器的成功与异常分支。
 * <p>
 * 测试环境未注册任何系统级 Authenticator，故需要认证的路径必定返回 401。
 */
public class AuthManagerTest {

    @Test
    public void sameHandlerInstanceAtTwoPaths_authPath_stillRequiresAuth() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            HttpHandler handler = (request, response) -> response.send("leak").finish();
            testServer.server.registerHttpHandlerNoAuth(handler, "/noAuthPath");
            testServer.server.registerHttpHandler(handler, "/authPath"); // 同一实例，认证要求不得被串味

            Assert.assertEquals(statusOf(testServer, "/noAuthPath"), 200);
            Assert.assertEquals(statusOf(testServer, "/authPath"), 401);
        } finally {
            testServer.server.stop();
        }
    }

    private static int statusOf(TestServer testServer, String path) throws Exception {
        HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
        Response result = client.send(client.newRequest("http://localhost:" + testServer.port + path)
                .method(HttpMethod.GET));
        return result.getStatus();
    }
}
