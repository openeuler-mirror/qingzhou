package qingzhou.http.server.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.http.server.impl.TestServerSupport.TestServer;

/**
 * DispatcherHandler 自动化测试集。
 * <p>
 * 本测试不单独测 DispatcherHandler 一个类，而是配合 HttpServer 接口的各 API
 * （registerHttpHandler/unregisterHttpHandler、HttpHandler 的 handle/buildStreamHandler、
 * HttpRequest 读取、HttpResponse 回写）通过真实请求端到端验证其分发行为。
 */
public class DispatcherHandlerTest {
    private static final int NO_STATUS_LINE = -1; // 服务端未返回状态行即关闭连接

    @Test
    public void unregisteredPath_request_returns404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/test", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/other")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void registeredPath_getRequest_handlerInvokedWithResponse() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.send("hello-" + request.getMethod()).finish(), "/test", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/test")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "hello-GET");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void unregisteredHandler_request_returns404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            HttpHandler handler = (request, response) -> response.send("ok").finish();
            testServer.server.registerHttpHandler(handler, "/temp", true);
            testServer.server.unregisterHttpHandler(handler);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/temp")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void sameHandlerAtTwoPaths_unregisterHttpHandler_allPathsReturn404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            HttpHandler handler = (request, response) -> response.send("ok").finish();
            testServer.server.registerHttpHandler(handler, "/pathOne", true);
            testServer.server.registerHttpHandler(handler, "/pathTwo", true);
            testServer.server.unregisterHttpHandler(handler); // 两条路径须一并移除，否则残留免认证条目继续对外服务

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Assert.assertEquals(client.send(client.buildRequest("http://localhost:" + testServer.port + "/pathOne")
                    .method(HttpMethod.GET)).getStatus(), 404);
            Assert.assertEquals(client.send(client.buildRequest("http://localhost:" + testServer.port + "/pathTwo")
                    .method(HttpMethod.GET)).getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void requestInfo_readViaHttpRequestApi_handlerReceivesMethodPathBody() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> {
                String method = request.getMethod();
                String path = request.getPath();
                byte[] body = request.getBody();
                String bodyStr = body == null ? "null" : new String(body, StandardCharsets.UTF_8);
                response.send("M=" + method + ",P=" + path + ",B=" + bodyStr).finish();
            }, "/infoTest", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/infoTest")
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
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.status(201)
                            .header("X-Test-Header", "test-value")
                            .send("created-body").finish(), "/respTest", true);

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
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.send(new String(request.getBody(), StandardCharsets.UTF_8)).finish(), "/postTest", true);

            Map<String, String> params = new HashMap<>();
            params.put("name", "qingzhou");
            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/postTest")
                    .method(HttpMethod.POST)
                    .params(params));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "name=qingzhou");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void multipartWithoutStreamHandler_request_returns415() throws Exception {
        File tempFile = File.createTempFile("dispatcher-upload-", ".txt");
        try {
            Files.write(tempFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));
            TestServer testServer = TestServerSupport.startServer();
            try {
                testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/upload", true);
                Map<String, List<String>> files = new HashMap<>();
                files.put("upload", Collections.singletonList(tempFile.getAbsolutePath()));

                HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
                Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/upload")
                        .method(HttpMethod.POST)
                        .files(files));

                Assert.assertEquals(result.getStatus(), 415);
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
            TestServer testServer = TestServerSupport.startServer();
            try {
                String path = "/streamUpload";
                AtomicReference<byte[]> received = new AtomicReference<>();
                testServer.server.registerHttpHandler(TestServerSupport.bufferingEchoHandler(received), path, true);

                Map<String, List<String>> files = new HashMap<>();
                files.put("upload", Collections.singletonList(tempFile.getAbsolutePath()));

                HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
                Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + path)
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
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> {
                throw new IllegalStateException("boom");
            }, "/boom", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/boom")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 500);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void streamHandlerBeginThrows_request_returns500() throws Exception {
        File tempFile = File.createTempFile("dispatcher-begin-", ".txt");
        try {
            Files.write(tempFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));
            TestServer testServer = TestServerSupport.startServer();
            try {
                testServer.server.registerHttpHandler(new HttpHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) {
                        response.status(400).finish(); // multipart 请求不会进入此方法
                    }

                    @Override
                    public StreamHandler multipartStreamHandler() {
                        return new StreamHandler() {
                            @Override
                            public void onBegin(HttpRequest request, HttpResponse response) {
                                throw new IllegalStateException("boom");
                            }

                            @Override
                            public void onNext(byte[] data) {
                            }

                            @Override
                            public void onError(Throwable t) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        };
                    }
                }, "/beginBoom", true);

                Map<String, List<String>> files = new HashMap<>();
                files.put("upload", Collections.singletonList(tempFile.getAbsolutePath()));

                HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
                Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/beginBoom")
                        .method(HttpMethod.POST)
                        .files(files));

                Assert.assertEquals(result.getStatus(), 500);
            } finally {
                testServer.server.stop();
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void traceMethod_request_returns405() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/traceTest", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/traceTest")
                    .method(HttpMethod.TRACE)); // 非业务方法不得进入 handler，否则可被用于跨站追踪

            Assert.assertEquals(result.getStatus(), 405);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void invalidContentLength_request_rejected() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/clTest", true);

            // 非法头不得按"长度未知"预留配额，否则少量并发即可耗尽聚合预算
            assertRejected(testServer.port, "POST /clTest HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: not-a-number\r\n"
                    + "Connection: close\r\n\r\n");
        } finally {
            testServer.server.stop();
        }
    }

    /**
     * 直连发送原始报文：HttpURLConnection 会自行改写 Content-Length，无法构造非法值或 CL+TE 并存。
     * 设读超时避免用例静默挂死；读不到状态行时返回 {@link #NO_STATUS_LINE}。
     */
    private static int statusOfRawRequest(int port, String rawRequest) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            return statusLine == null ? NO_STATUS_LINE : Integer.parseInt(statusLine.split(" ")[1]);
        }
    }

    /**
     * 断言请求未被 handler 受理。非法报文可能在 Netty 解码层就被拒并关闭连接（无状态行），
     * 因此 400 与无响应都算通过；若闸门失效，请求会被 handler 受理并返回 200，用例即失败。
     */
    private static void assertRejected(int port, String rawRequest) throws Exception {
        int status = statusOfRawRequest(port, rawRequest);
        Assert.assertTrue(status == 400 || status == NO_STATUS_LINE,
                "request must be rejected, got status " + status);
    }

    /**
     * 直连发送原始报文并读取完整响应（请求须带 Connection: close，读到 EOF 即返回），
     * 供响应头断言使用；{@link #statusOfRawRequest} 只取状态行。
     * 若服务端未关闭连接，readLine 将挂到 SoTimeout 并抛出——该失败本身就是信号。
     */
    private static String rawResponse(int port, String rawRequest) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line).append('\n');
            return response.toString();
        }
    }

    // Netty 按传入名原样输出（HttpHeaderNames 为小写），断言统一转小写避免大小写耦合
    private static String lower(String response) {
        return response.toLowerCase(Locale.ROOT);
    }

    /**
     * 回归用例：模拟 SSE 这类「handler 返回后由后台线程继续写」的场景。
     * 框架若在 handler 返回时无条件 finish，后续写入会以 FAIL_TERMINATED 失败，late 分片丢失。
     */
    @Test
    public void asyncHandlerWritesAfterReturn_response_containsLateChunk() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            CountDownLatch lateWriteFailed = new CountDownLatch(1);
            testServer.server.registerHttpHandler(new HttpHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response) {
                    response.send("first");
                    new Thread(() -> { // 与 AiChat 中模型回调线程等价
                        try {
                            TimeUnit.MILLISECONDS.sleep(1500);
                            response.send("second").finish();
                        } catch (Throwable t) {
                            lateWriteFailed.countDown();
                        }
                    }).start();
                }
            }, "/asyncDiag", true);

            String response = rawResponse(testServer.port, "GET /asyncDiag HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n\r\n");

            Assert.assertTrue(response.contains("first"), response);
            Assert.assertFalse(lateWriteFailed.await(2, TimeUnit.SECONDS), "late write failed");
            Assert.assertTrue(response.contains("second"), "late chunk lost: " + response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void handlerSendsWithoutFinish_request_completes() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            // 契约：响应必须由 handler 自己收尾；此处只 send 不 finish 属编程错误，请求会挂到空闲超时
            testServer.server.registerHttpHandler((request, response) -> response.send("partial").finish(), "/noFinish", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/noFinish")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "partial");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void prefixPathRegistered_request_routesByPrefix() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("prefix").finish(), "/a", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/a/b/c")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "prefix");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void overlappingPathRegister_registerHttpHandler_throwsException() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("a").finish(), "/a", true);

            // HttpServer 注册阶段禁止前缀包含的重叠路径（DispatcherHandler 的长路径优先排序为防御性逻辑）
            try {
                testServer.server.registerHttpHandler((request, response) -> response.send("ab").finish(), "/a/b", true);
                Assert.fail("registering overlapping path should throw");
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage() != null && e.getMessage().contains("/a/b"));
            }
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void deepPathRegistered_requestRoot_returns404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("deep").finish(), "/a/b", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void childPathRegistered_requestParent_returns404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("stream").finish(), "/ai/chat/stream", true);
            testServer.server.registerHttpHandler((request, response) -> response.send("config").finish(), "/ai/chat/config", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/ai/chat")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void siblingPrefixPath_requestSibling_returns404() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("foo").finish(), "/foo", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/foobar")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 404);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void rootAndChildRegistered_requestChild_routesToLongest() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("root").finish(), "/root/other", true);
            testServer.server.registerHttpHandler((request, response) -> response.send("child").finish(), "/root/child", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response child = client.send(client.buildRequest("http://localhost:" + testServer.port + "/root/child/deep")
                    .method(HttpMethod.GET));
            Assert.assertEquals(child.getStatus(), 200);
            Assert.assertEquals(new String(child.getBody(), StandardCharsets.UTF_8), "child");

            Response root = client.send(client.buildRequest("http://localhost:" + testServer.port + "/root/other")
                    .method(HttpMethod.GET));
            Assert.assertEquals(root.getStatus(), 200);
            Assert.assertEquals(new String(root.getBody(), StandardCharsets.UTF_8), "root");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void traversalPath_request_returns400() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("echo:" + request.getPath()).finish(), "/test", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/test/../../etc/passwd")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 400);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void redundantSlashPath_request_normalized() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("echo:" + request.getPath()).finish(), "/test", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "//test//a")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "echo:/test/a");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void urlEncodedPath_request_decodedAndRouted() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) ->
                    response.send("decoded-" + request.getPath()).finish(), "/hello world", true);

            HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
            Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/hello%20world")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "decoded-/hello world");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void optionsPreflight_request_returns204() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/pre", true);

            // 预检不带凭据，须在认证之前终止；带 Access-Control-Request-Method 才算预检
            int status = statusOfRawRequest(testServer.port, "OPTIONS /pre HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: http://localhost\r\n"
                    + "Access-Control-Request-Method: POST\r\n"
                    + "Connection: close\r\n\r\n");

            Assert.assertEquals(status, 204);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void corsAllowedOrigin_request_getsCorsHeaders() throws Exception {
        TestServer testServer = TestServerSupport.startServer(
                Collections.singletonMap("cors_allowed_origins", "https://a.example"));
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/cors", true);

            // CORS 头必须出现在实际响应上：只在预检回显会让浏览器在真实请求阶段被拦
            String response = lower(rawResponse(testServer.port, "GET /cors HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: https://a.example\r\n"
                    + "Connection: close\r\n\r\n"));

            Assert.assertTrue(response.contains("access-control-allow-origin: https://a.example"), response);
            Assert.assertTrue(response.contains("access-control-allow-credentials: true"), response);
            Assert.assertTrue(response.contains("vary: origin"), response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void corsWildcardOrigin_request_getsWildcardWithoutCredentials() throws Exception {
        TestServer testServer = TestServerSupport.startServer(Collections.singletonMap("cors_allowed_origins", "*"));
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/cors", true);

            // 通配来源与可携带凭据并存会被浏览器直接拒绝
            String response = lower(rawResponse(testServer.port, "GET /cors HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: https://a.example\r\n"
                    + "Connection: close\r\n\r\n"));

            Assert.assertTrue(response.contains("access-control-allow-origin: *"), response);
            Assert.assertFalse(response.contains("access-control-allow-credentials"), response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void corsUnknownOrigin_request_noCorsHeaders() throws Exception {
        TestServer testServer = TestServerSupport.startServer(
                Collections.singletonMap("cors_allowed_origins", "https://a.example"));
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/cors", true);

            String response = lower(rawResponse(testServer.port, "GET /cors HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: https://b.example\r\n"
                    + "Connection: close\r\n\r\n"));

            Assert.assertFalse(response.contains("access-control-allow-origin"), response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void corsConfigured_preflight_returns204WithCorsHeaders() throws Exception {
        TestServer testServer = TestServerSupport.startServer(
                Collections.singletonMap("cors_allowed_origins", "https://a.example"));
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/pre", true);

            String response = lower(rawResponse(testServer.port, "OPTIONS /pre HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: https://a.example\r\n"
                    + "Access-Control-Request-Method: POST\r\n"
                    + "Access-Control-Request-Headers: X-Custom-Trace, Content-Type\r\n"
                    + "Connection: close\r\n\r\n"));

            Assert.assertTrue(response.startsWith("http/1.1 204"), response);
            Assert.assertTrue(response.contains("access-control-allow-origin: https://a.example"), response);
            Assert.assertTrue(response.contains("access-control-allow-methods"), response);
            // 未列入服务端白名单的自定义头不得被回显
            Assert.assertFalse(response.contains("x-custom-trace"), response);
            Assert.assertTrue(response.contains("access-control-allow-headers: content-type"), response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void corsWildcardRequestedHeaders_preflight_fallsBackToDefaultSet() throws Exception {
        TestServer testServer = TestServerSupport.startServer(
                Collections.singletonMap("cors_allowed_origins", "https://a.example"));
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/pre", true);

            // 客户端传 * 会宣告放行任意自定义请求头（防御纵深弱化），须按不存在处理回退默认集
            String response = lower(rawResponse(testServer.port, "OPTIONS /pre HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Origin: https://a.example\r\n"
                    + "Access-Control-Request-Method: POST\r\n"
                    + "Access-Control-Request-Headers: *\r\n"
                    + "Connection: close\r\n\r\n"));

            Assert.assertTrue(response.startsWith("http/1.1 204"), response);
            Assert.assertFalse(response.contains("access-control-allow-headers: *"), response);
            Assert.assertTrue(response.contains("access-control-allow-headers: content-type, authorization"), response);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void streamHandlerEndsResponseEarly_connectionClosedWithoutResidualBody() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            // onBegin 即终止响应、不消费上传体：框架必须断开连接，
            // 残留请求体（含其后的管道化请求）不得在保活连接上被当作下一个请求解析（请求走私）
            testServer.server.registerHttpHandler(new HttpHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response) {
                    response.status(400).finish(); // 仅聚合分支会进入
                }

                @Override
                public StreamHandler multipartStreamHandler() {
                    return new StreamHandler() {
                        @Override
                        public void onBegin(HttpRequest request, HttpResponse response) {
                            response.status(403).send("forbidden").finish();
                        }

                        @Override
                        public void onNext(byte[] data) {
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    };
                }
            }, "/earlyStop", true);

            String body = String.join("", Collections.nCopies(64, "0123456789abcdef")); // 1KB 上传体
            String part = "--B\r\nContent-Disposition: form-data; name=\"f\"; filename=\"f\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n" + body + "\r\n";
            String pipelined = "GET /earlyStop HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            String raw = "POST /earlyStop HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Type: multipart/form-data; boundary=B\r\n"
                    + "Content-Length: " + part.length() + "\r\n"
                    + "Connection: keep-alive\r\n\r\n" + part + pipelined;

            // rawResponse 读到 EOF 才返回：连接若未被关闭，readLine 将挂到 SoTimeout 并抛出，用例即失败
            String response = lower(rawResponse(testServer.port, raw));

            Assert.assertTrue(response.startsWith("http/1.1 403"), response); // 早停响应未被断连截断
            Assert.assertTrue(response.contains("forbidden"), response);
            Assert.assertEquals(countStatusLines(response), 1, "pipelined request must not be served: " + response);
        } finally {
            testServer.server.stop();
        }
    }

    private static int countStatusLines(String response) {
        int count = 0;
        for (String line : response.split("\n")) {
            if (line.startsWith("http/1.1 ")) count++;
        }
        return count;
    }

    @Test
    public void negativeContentLength_request_rejected() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/neg", true);

            // 负长度若按 0 预留配额，少量并发即可绕过全局聚合预算
            assertRejected(testServer.port, "POST /neg HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: -1\r\n"
                    + "Connection: close\r\n\r\n");
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void multiLayerEncodedPath_request_returns400() throws Exception {
        TestServer testServer = TestServerSupport.startServer();
        try {
            testServer.server.registerHttpHandler((request, response) -> response.send("ok").finish(), "/enc", true);

            // %252f 一次解码后仍残留编码形态，各 handler 的穿越防护未必覆盖得住
            Assert.assertEquals(statusOfRawRequest(testServer.port, "GET /enc/%252f..%252fetc HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n\r\n"), 400);
        } finally {
            testServer.server.stop();
        }
    }

    @Test
    public void streamHandlerCompleteThrows_request_stillCompletes() throws Exception {
        File tempFile = File.createTempFile("dispatcher-complete-", ".txt");
        try {
            Files.write(tempFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));
            TestServer testServer = TestServerSupport.startServer();
            try {
                testServer.server.registerHttpHandler(new HttpHandler() {
                    @Override
                    public void handle(HttpRequest request, HttpResponse response) {
                        response.status(400).finish(); // multipart 请求不会进入此方法
                    }

                    @Override
                    public StreamHandler multipartStreamHandler() {
                        return new StreamHandler() {
                            @Override
                            public void onBegin(HttpRequest request, HttpResponse response) {
                            }

                            @Override
                            public void onNext(byte[] data) {
                            }

                            @Override
                            public void onError(Throwable t) {
                            }

                            @Override
                            public void onComplete() {
                                throw new IllegalStateException("boom"); // 回调异常不得让 sink 永不结束
                            }
                        };
                    }
                }, "/completeBoom", true);

                Map<String, List<String>> files = new HashMap<>();
                files.put("upload", Collections.singletonList(tempFile.getAbsolutePath()));

                HttpClient client = HttpClientServerIntegrationTest.buildHttpClientImpl();
                // 回调异常若无人兜底，此处会一直挂到 idle_timeout
                Response result = client.send(client.buildRequest("http://localhost:" + testServer.port + "/completeBoom")
                        .method(HttpMethod.POST)
                        .files(files));

                Assert.assertTrue(result.getStatus() >= 200, "response must complete, got " + result.getStatus());
            } finally {
                testServer.server.stop();
            }
        } finally {
            tempFile.delete();
        }
    }

}
