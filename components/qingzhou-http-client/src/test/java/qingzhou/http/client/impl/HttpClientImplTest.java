package qingzhou.http.client.impl;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

public class HttpClientImplTest {
    static HttpClientImpl buildHttpClientImpl() {
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.activate();
        return httpClient;
    }

    @Test
    public void unsupportedProtocol_send_throwsIllegalArgumentException() {
        HttpClient client = buildHttpClientImpl();
        try {
            client.send(client.newRequest("ftp://127.0.0.1/file"));
            Assert.fail("不支持的协议应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("unsupported protocol"), "实际异常：" + e);
        } catch (Exception e) {
            Assert.fail("期望 IllegalArgumentException，实际：" + e);
        }
    }

    @Test
    public void getRequest_send_responseReturned() throws Exception {
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        withServer(exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            writeResponse(exchange, 200, "get-success");
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "get-success");
            Assert.assertEquals(receivedMethod.get(), HttpMethod.GET.name());
        });
    }

    @Test
    public void postRequest_send_responseReturned() throws Exception {
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        withServer(exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            writeResponse(exchange, 200, "post-success");
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.POST));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "post-success");
            Assert.assertEquals(receivedMethod.get(), HttpMethod.POST.name());
        });
    }

    @Test
    public void requestHeader_send_serverReceivesHeader() throws Exception {
        AtomicReference<String> receivedHeader = new AtomicReference<>();
        withServer(exchange -> {
            receivedHeader.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            writeResponse(exchange, 200, "header-received");
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url)
                    .method(HttpMethod.GET)
                    .header("X-Request-Id", "request-123"));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(receivedHeader.get(), "request-123");
        });
    }

    @Test
    public void requestBody_send_serverReceivesBody() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        withServer(exchange -> {
            receivedBody.set(readRequestBody(exchange));
            writeResponse(exchange, 200, "body-received");
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url)
                    .method(HttpMethod.POST)
                    .body("name=qingzhou".getBytes(StandardCharsets.UTF_8)));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(receivedBody.get(), "name=qingzhou");
        });
    }

    @Test
    public void createdResponse_getStatus_returnsResponseStatus() throws Exception {
        withServer(exchange -> writeResponse(exchange, 201, "created"), url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET));

            Assert.assertEquals(response.getStatus(), 201);
        });
    }

    @Test
    public void responseWithBody_getBody_returnsResponseContent() throws Exception {
        withServer(exchange -> writeResponse(exchange, 200, "response-content"), url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET));

            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "response-content");
        });
    }

    @Test
    public void successfulResponse_sendWithListener_deliversAsynchronously() throws Exception {
        CountDownLatch responseHeadersSent = new CountDownLatch(1);
        CountDownLatch responseBodyAllowed = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> receivedLine = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        withServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.flush();
                responseHeadersSent.countDown();
                await(responseBodyAllowed);
                output.write("async-response\n".getBytes(StandardCharsets.UTF_8));
            }
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            try {
                Response response = client.send(client.newRequest(url).method(HttpMethod.GET), new ResponseListener() {
                    @Override
                    public void onBody(String line) {
                        receivedLine.set(line);
                    }

                    @Override
                    public void onComplete() {
                        completed.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        completed.countDown();
                    }
                });

                Assert.assertEquals(response.getStatus(), 200);
                Assert.assertTrue(responseHeadersSent.await(5, TimeUnit.SECONDS), "response headers were not sent");
                Assert.assertEquals(completed.getCount(), 1L, "listener completed before send returned");
                responseBodyAllowed.countDown();
                Assert.assertTrue(completed.await(5, TimeUnit.SECONDS), "asynchronous response did not complete");
                Assert.assertNull(error.get());
                Assert.assertEquals(receivedLine.get(), "async-response");
            } finally {
                responseBodyAllowed.countDown();
            }
        });
    }

    @Test
    public void errorResponse_sendWithListener_callsOnError() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        withServer(exchange -> writeResponse(exchange, 500, "server-error"), url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET), new ResponseListener() {
                @Override
                public void onBody(String line) {
                    Assert.fail("非 2xx 不应回调 onBody");
                }

                @Override
                public void onComplete() {
                    Assert.fail("非 2xx 不应回调 onComplete");
                }

                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                }
            });

            Assert.assertEquals(response.getStatus(), 500);
            Assert.assertNotNull(error.get());
            Assert.assertEquals(new String(response.getBody(), StandardCharsets.UTF_8), "server-error");
        });
    }

    @Test
    public void oversizedBody_exceedingMaxBodySize_throwsIOException() throws Exception {
        withServer(exchange -> writeResponse(exchange, 200, "0123456789abcdef"), url -> {
            HttpClient client = buildHttpClientImpl();
            try {
                client.send(client.newRequest(url).method(HttpMethod.GET).maxBodySize(8));
                Assert.fail("响应体超过 maxBodySize 应抛出 IOException");
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage().contains("maxBodySize"), "实际异常：" + e);
            }
        });
    }

    @Test
    public void multipartFieldWithCrlf_send_throwsIllegalArgumentException() throws Exception {
        withServer(exchange -> writeResponse(exchange, 200, "ok"), url -> {
            HttpClient client = buildHttpClientImpl();
            File tempFile = File.createTempFile("multipart-inject-", ".txt");
            try {
                Map<String, List<String>> files = new HashMap<>();
                files.put("field\r\nX-Injected: 1", Collections.singletonList(tempFile.getAbsolutePath()));

                client.send(client.newRequest(url).method(HttpMethod.POST).files(files));
                Assert.fail("含 CRLF 的 multipart 字段名应抛出 IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                Assert.assertTrue(e.getMessage().contains("illegal multipart"), "实际异常：" + e);
            } finally {
                tempFile.delete();
            }
        });
    }

    @Test
    public void paramsWithSpecialChars_send_serverReceivesEncodedBody() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedContentType = new AtomicReference<>();
        withServer(exchange -> {
            receivedBody.set(readRequestBody(exchange));
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            writeResponse(exchange, 200, "ok");
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Map<String, String> params = new HashMap<>();
            params.put("q", "a b&c=d");

            Response response = client.send(client.newRequest(url).method(HttpMethod.POST).params(params));

            Assert.assertEquals(response.getStatus(), 200);
            Assert.assertEquals(receivedBody.get(), "q=a+b%26c%3Dd");
            Assert.assertEquals(receivedContentType.get(), "application/x-www-form-urlencoded");
        });
    }

    @Test
    public void streamingResponse_cancel_stopsCallbacksAndDisconnects() throws Exception {
        CountDownLatch firstLine = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();
        withServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write("line-1\n".getBytes(StandardCharsets.UTF_8));
                output.flush();
                Thread.sleep(1000); // 挂住服务端，等待客户端取消
            } catch (InterruptedException ignored) {
            }
        }, url -> {
            HttpClient client = buildHttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET), new ResponseListener() {
                @Override
                public void onBody(String line) {
                    firstLine.countDown();
                }

                @Override
                public void onComplete() {
                    completed.set(true);
                }

                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                }
            });

            Assert.assertTrue(firstLine.await(5, TimeUnit.SECONDS), "did not receive first line");
            response.cancel();
            Thread.sleep(500);
            Assert.assertFalse(completed.get(), "取消后不应回调 onComplete");
            Assert.assertNull(error.get(), "取消后不应回调 onError");
        });
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("response body was not released");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("response body wait was interrupted", exception);
        }
    }

    private void withServer(HttpHandler handler, ServerTest test) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        try {
            test.run("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        } finally {
            server.stop(0);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            for (int read; (read = input.read(buffer)) != -1; ) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private interface ServerTest {
        void run(String url) throws Exception;
    }
}
