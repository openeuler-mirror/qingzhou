package qingzhou.http.impl;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import reactor.netty.DisposableServer;

/**
 * http-server 测试公共夹具：端口 0 启动 + 实际端口读取 + 缓冲回显的流式 handler。
 */
final class TestServerSupport {
    private TestServerSupport() {
    }

    static TestServer startServer() throws Exception {
        HttpServerImpl httpServer = HttpServerImplTest.build(0); // 端口 0：由操作系统分配空闲端口
        try {
            Field field = HttpServerImpl.class.getDeclaredField("disposableServer");
            field.setAccessible(true);
            DisposableServer disposableServer = (DisposableServer) field.get(httpServer);
            return new TestServer(httpServer, ((InetSocketAddress) disposableServer.address()).getPort());
        } catch (Exception e) {
            httpServer.stop(); // 端口读取失败时也不能泄漏已启动的服务
            throw e;
        }
    }

    // 缓冲全部上传字节并在完成后原样回写，供断言比对 multipart 内容
    static HttpHandler bufferingEchoHandler(AtomicReference<byte[]> received) {
        return new HttpHandler() {
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
                        received.set(body);
                        httpResponse.sendFinish(body);
                    }
                };
            }
        };
    }

    static class TestServer {
        final HttpServerImpl server;
        final int port;

        TestServer(HttpServerImpl server, int port) {
            this.server = server;
            this.port = port;
        }
    }
}
