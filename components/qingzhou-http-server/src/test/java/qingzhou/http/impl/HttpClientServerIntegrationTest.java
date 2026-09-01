package qingzhou.http.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
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
 * 端到端集成测试：验证 HttpClientImpl 发送的请求头和文件能被 HttpServerImpl 正确接收。
 * <p>
 * 注：本测试位于 qingzhou-http-server 模块，因该模块已以 test 范围依赖 qingzhou-http-client；
 * 若反向依赖则形成 Maven 循环依赖。
 */
public class HttpClientServerIntegrationTest {

    @Test
    public void setHeader_send_serverReceivesHeader() throws Exception {
        TestServer testServer = startServer();
        int port = testServer.port;
        HttpServerImpl httpServer = testServer.server;
        try {
            String path = "/headerTest";
            httpServer.registerHttpHandlerNoAuth((request, response) ->
                    response.sendFinish("received: " + request.getHeader("X-Request-Id")), path);

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + port + path)
                    .method(HttpMethod.GET)
                    .header("X-Request-Id", "request-123"));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "received: request-123");
        } finally {
            httpServer.stop();
        }
    }

    @Test
    public void setHeaders_send_serverReceivesAllHeaders() throws Exception {
        TestServer testServer = startServer();
        int port = testServer.port;
        HttpServerImpl httpServer = testServer.server;
        try {
            String path = "/headersTest";
            httpServer.registerHttpHandlerNoAuth((request, response) -> {
                String headerA = request.getHeader("Header-A");
                String headerB = request.getHeader("Header-B");
                String oldHeader = request.getHeader("Old-Header");
                response.sendFinish("A=" + headerA + ",B=" + headerB + ",Old=" + oldHeader);
            }, path);

            Map<String, String> newHeaders = new HashMap<>();
            newHeaders.put("Header-A", "value-a");
            newHeaders.put("Header-B", "value-b");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + port + path)
                    .method(HttpMethod.GET)
                    .header("Old-Header", "old-value")
                    .headers(newHeaders));

            // headers(Map) 整体替换后，先前 header() 设置的头不再发送；服务端未收到时 getHeader 返回 null
            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8),
                    "A=value-a,B=value-b,Old=null");
        } finally {
            httpServer.stop();
        }
    }

    @Test
    public void setFiles_send_serverReceivesMultipartContent() throws Exception {
        File file1 = File.createTempFile("qingzhou-upload1-", ".txt");
        File file2 = File.createTempFile("qingzhou-upload2-", ".txt");
        try {
            Files.write(file1.toPath(), "file-content-one".getBytes(StandardCharsets.UTF_8));
            Files.write(file2.toPath(), "file-content-two".getBytes(StandardCharsets.UTF_8));

            TestServer testServer = startServer();
            int port = testServer.port;
            HttpServerImpl httpServer = testServer.server;
            try {
                String path = "/filesTest";
                AtomicReference<byte[]> receivedBody = new AtomicReference<>();
                httpServer.registerHttpHandlerNoAuth(new HttpHandler() {
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
                                buffer.write(data, 0, data.length); // ByteArrayOutputStream.write 不抛出 IOException
                            }

                            @Override
                            public void onError(Throwable t) {
                                httpResponse.status500Finish(t.getMessage());
                            }

                            @Override
                            public void onComplete() {
                                byte[] body = buffer.toByteArray();
                                receivedBody.set(body);
                                httpResponse.sendFinish(body); // 原样回写收到的 multipart 字节，供客户端断言
                            }
                        };
                    }
                }, path);

                Map<String, String> files = new HashMap<>();
                files.put("upload", file1.getAbsolutePath() + "," + file2.getAbsolutePath());
                Map<String, String> params = new HashMap<>();
                params.put("desc", "unit-test");

                HttpClient client = new HttpClientImpl();
                Response result = client.send(client.newRequest("http://localhost:" + port + path)
                        .method(HttpMethod.POST)
                        .files(files)
                        .params(params));

                Assert.assertEquals(result.getStatus(), 200);
                Assert.assertNotNull(receivedBody.get());
                String body = new String(receivedBody.get(), StandardCharsets.UTF_8);

                // 文本字段部分
                Assert.assertTrue(body.contains("Content-Disposition: form-data; name=\"desc\""), "text field part missing");
                Assert.assertTrue(body.contains("unit-test"), "text field value missing");

                // 文件部分：文件名、根据扩展名猜测的 Content-Type、文件内容
                Assert.assertTrue(body.contains("Content-Disposition: form-data; name=\"upload\"; filename=\"" + file1.getName() + "\""),
                        "first file part missing");
                Assert.assertTrue(body.contains("Content-Disposition: form-data; name=\"upload\"; filename=\"" + file2.getName() + "\""),
                        "second file part missing");
                Assert.assertTrue(body.contains("Content-Type: text/plain"), "guessed content type missing");
                Assert.assertTrue(body.contains("file-content-one"), "first file content missing");
                Assert.assertTrue(body.contains("file-content-two"), "second file content missing");
            } finally {
                httpServer.stop();
            }
        } finally {
            file1.delete();
            file2.delete();
        }
    }

    /**
     * 以端口 0 启动服务端，由操作系统分配空闲端口，再从 disposableServer 读取实际端口。
     * 避免固定端口在重复运行或并行执行时冲突导致的偶发失败。
     */
    private TestServer startServer() throws Exception {
        HttpServerImpl httpServer = HttpServerImplTest.build(0);
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
