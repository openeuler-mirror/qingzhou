package qingzhou.http.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.impl.TestServerSupport.TestServer;
import qingzhou.http.server.*;

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
            HttpHandler handler = (request, response) -> response.sendFinish("leak");
            testServer.server.registerHttpHandlerNoAuth(handler, "/noAuthPath");
            testServer.server.registerHttpHandler(handler, "/authPath"); // 同一实例，认证要求不得被串味

            Assert.assertEquals(statusOf(testServer, "/noAuthPath"), 200);
            Assert.assertEquals(statusOf(testServer, "/authPath"), 401);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void customAuthenticatorThrows_request_returns401() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler(new HttpHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response) {
                    response.sendFinish("ok");
                }

                @Override
                public Authenticator customAuthenticator() {
                    return request -> {
                        throw new IllegalStateException("boom");
                    };
                }
            }, "/throwingAuth");

            Assert.assertEquals(statusOf(testServer, "/throwingAuth"), 401);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void customAuthenticatorPass_request_returns200() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler(new HttpHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response) {
                    response.sendFinish("ok");
                }

                @Override
                public Authenticator customAuthenticator() {
                    return request -> AuthResult.pass("tester", new String[]{"admin"});
                }
            }, "/customAuth");

            Assert.assertEquals(statusOf(testServer, "/customAuth"), 200);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void customAuthenticatorReject_request_returns401() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler(new HttpHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response) {
                    response.sendFinish("ok");
                }

                @Override
                public Authenticator customAuthenticator() {
                    return request -> AuthResult.reject("invalid token");
                }
            }, "/rejectingAuth");

            Assert.assertEquals(statusOf(testServer, "/rejectingAuth"), 401);
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
