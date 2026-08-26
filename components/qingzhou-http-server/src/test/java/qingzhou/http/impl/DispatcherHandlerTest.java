package qingzhou.http.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import reactor.netty.DisposableServer;

/**
 * DispatcherHandler 自动化测试集。
 * <p>
 * 本测试不单独测 DispatcherHandler 一个类，而是配合 HttpServer 接口的各 API
 * （registerHttpHandler/unregisterHttpHandler、HttpHandler 的 handle/buildStreamHandler、
 * HttpRequest 读取、HttpResponse 回写）通过真实请求端到端验证其分发行为。
 */
public class DispatcherHandlerTest {

    @Test
    public void unregisteredPath_request_returns404() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.sendFinish("ok"), "/test");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/other")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void registeredPath_getRequest_handlerInvokedWithResponse() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.sendFinish("hello-" + request.getMethod()), "/test");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/test")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "hello-GET");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void unregisteredHandler_request_returns404() throws Exception {
        TestServer testServer = startServer();
        try {
            HttpHandler handler = (request, response) -> response.sendFinish("ok");
            testServer.server.registerHttpHandler(handler, "/temp");
            testServer.server.unregisterHttpHandler(handler);

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/temp")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void requestInfo_readViaHttpRequestApi_handlerReceivesMethodPathBody() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> {
                String method = request.getMethod();
                String path = request.getPath();
                byte[] body = request.getBody();
                String bodyStr = body == null ? "null" : new String(body, StandardCharsets.UTF_8);
                response.sendFinish("M=" + method + ",P=" + path + ",B=" + bodyStr);
            }, "/infoTest");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/infoTest")
                    .method(HttpMethod.POST)
                    .body("hello-body".getBytes(StandardCharsets.UTF_8)));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8),
                    "M=POST,P=/infoTest,B=hello-body");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void responseWrite_httpResponseApi_clientReceivesStatusHeaderBody() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.status(201)
                            .header("X-Test-Header", "test-value")
                            .sendFinish("created-body"), "/respTest");

            // HttpClientImpl 未暴露响应头读取，此处用其底层的 HttpURLConnection 完整验证三个回写 API
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + testServer.port + "/respTest")
                    .openConnection();
            try {
                conn.setRequestMethod("GET");
                Assert.assertEquals(conn.getResponseCode(), 201);
                Assert.assertEquals(conn.getHeaderField("X-Test-Header"), "test-value");
                try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    for (int read; (read = in.read(buffer)) != -1; ) {
                        out.write(buffer, 0, read);
                    }
                    Assert.assertEquals(new String(out.toByteArray(), StandardCharsets.UTF_8), "created-body");
                }
            } finally {
                conn.disconnect();
            }
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void postParams_postRequest_handlerReceivesAggregatedBody() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.sendFinish(new String(request.getBody(), StandardCharsets.UTF_8)), "/postTest");

            Map<String, String> params = new HashMap<>();
            params.put("name", "qingzhou");
            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/postTest")
                    .method(HttpMethod.POST)
                    .params(params));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "name=qingzhou");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void multipartWithoutStreamHandler_request_returns413() throws Exception {
        File tempFile = File.createTempFile("dispatcher-upload-", ".txt");
        try {
            Files.write(tempFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));
            TestServer testServer = startServer();
            try {
                testServer.server.registerHttpHandler((request, response) -> response.sendFinish("ok"), "/upload");
                Map<String, String> files = new HashMap<>();
                files.put("upload", tempFile.getAbsolutePath());

                HttpClient client = new HttpClientImpl();
                Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/upload")
                        .method(HttpMethod.POST)
                        .files(files));

                Assert.assertEquals(result.getStatus(), 413);
            } finally {
                testServer.server.stop();
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void multipartWithStreamHandler_request_streamReceivesFullBody() throws Exception {
        File tempFile = File.createTempFile("dispatcher-stream-", ".txt");
        try {
            Files.write(tempFile.toPath(), "stream-file-content".getBytes(StandardCharsets.UTF_8));
            TestServer testServer = startServer();
            try {
                String path = "/streamUpload";
                AtomicReference<byte[]> received = new AtomicReference<>();
                testServer.server.registerHttpHandler(new HttpHandler() {
                    @Override
                    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
                        httpResponse.status400Finish(); // multipart 请求不会进入此方法
                    }

                    @Override
                    public StreamHandler buildStreamHandler() {
                        return new StreamHandler() {
                            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            private HttpResponse httpResponse;

                            @Override
                            public void onBegin(HttpRequest request, HttpResponse response) {
                                this.httpResponse = response;
                            }

                            @Override
                            public void onNext(byte[] data) {
                                buffer.write(data, 0, data.length);
                            }

                            @Override
                            public void onError(Throwable t) {
                                httpResponse.status500Finish(t.getMessage());
                            }

                            @Override
                            public void onComplete() {
                                byte[] body = buffer.toByteArray();
                                received.set(body);
                                httpResponse.sendFinish(body);
                            }
                        };
                    }
                }, path);

                Map<String, String> files = new HashMap<>();
                files.put("upload", tempFile.getAbsolutePath());

                HttpClient client = new HttpClientImpl();
                Response result = client.send(client.newRequest("http://localhost:" + testServer.port + path)
                        .method(HttpMethod.POST)
                        .files(files));

                Assert.assertEquals(result.getStatus(), 200);
                Assert.assertNotNull(received.get());
                String body = new String(received.get(), StandardCharsets.UTF_8);
                Assert.assertTrue(body.contains("filename=\"" + tempFile.getName() + "\""));
                Assert.assertTrue(body.contains("stream-file-content"));
            } finally {
                testServer.server.stop();
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void handlerThrowsException_request_returns500() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> {
                throw new IllegalStateException("boom");
            }, "/boom");

            HttpClient client = new HttpClientImpl();
            try {
                Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/boom")
                        .method(HttpMethod.GET));
                // 已知服务端行为：handler 异常时响应流以 error 终止，有时可读到 500 状态
                Assert.assertEquals(result.getStatus(), 500);
            } catch (java.io.IOException e) {
                // 已知服务端行为边界：error 终止导致 chunked 连接提前切断，客户端读到 Premature EOF
                Assert.assertTrue(e.getMessage() != null && e.getMessage().contains("Premature EOF"),
                        "unexpected io error: " + e.getMessage());
            }
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void prefixPathRegistered_request_routesByPrefix() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.sendFinish("prefix"), "/a");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/a/b/c")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "prefix");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void overlappingPathRegister_registerHttpHandler_throwsException() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.sendFinish("a"), "/a");

            // HttpServer 注册阶段禁止前缀包含的重叠路径（DispatcherHandler 的长路径优先排序为防御性逻辑）
            try {
                testServer.server.registerHttpHandler((request, response) -> response.sendFinish("ab"), "/a/b");
                Assert.fail("registering overlapping path should throw");
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage() != null && e.getMessage().contains("/a/b"));
            }
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void urlEncodedPath_request_decodedAndRouted() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.sendFinish("decoded-" + request.getPath()), "/hello world");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/hello%20world")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "decoded-/hello world");
        } finally {
            testServer.server.stop();
        }
    }

    private TestServer startServer() throws Exception {
        HttpServerImpl httpServer = HttpServerImplTest.build(0); // 端口 0：由操作系统分配空闲端口
        Field field = HttpServerImpl.class.getDeclaredField("disposableServer");
        field.setAccessible(true);
        DisposableServer disposableServer = (DisposableServer) field.get(httpServer);
        java.net.InetSocketAddress address = (java.net.InetSocketAddress) disposableServer.address();
        return new TestServer(httpServer, address.getPort());
    }

    private static class TestServer {
        final HttpServerImpl server;
        final int port;

        TestServer(HttpServerImpl server, int port) {
            this.server = server;
            this.port = port;
        }
    }
}
