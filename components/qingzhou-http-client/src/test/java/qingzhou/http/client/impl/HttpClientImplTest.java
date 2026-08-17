package qingzhou.http.client.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

public class HttpClientImplTest {
    @Test
    public void getRequest_send_responseReturned() throws Exception {
        AtomicReference<String> receivedMethod = new AtomicReference<>();
        withServer(exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            writeResponse(exchange, 200, "get-success");
        }, url -> {
            HttpClient client = new HttpClientImpl();
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
            HttpClient client = new HttpClientImpl();
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
            HttpClient client = new HttpClientImpl();
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
            HttpClient client = new HttpClientImpl();
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
            HttpClient client = new HttpClientImpl();
            Response response = client.send(client.newRequest(url).method(HttpMethod.GET));

            Assert.assertEquals(response.getStatus(), 201);
        });
    }

    @Test
    public void responseWithBody_getBody_returnsResponseContent() throws Exception {
        withServer(exchange -> writeResponse(exchange, 200, "response-content"), url -> {
            HttpClient client = new HttpClientImpl();
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
            HttpClient client = new HttpClientImpl();
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
