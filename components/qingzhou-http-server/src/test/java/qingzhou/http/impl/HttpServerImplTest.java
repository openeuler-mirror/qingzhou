package qingzhou.http.impl;

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.logger.impl.LoggerImpl;

public class HttpServerImplTest {
    @Test
    public void normal_start_listenHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        HttpResult result = new HttpClientImpl().request("http://localhost:" + port, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normal_stop_requestGetConnectException() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        httpServer.stop();
        try {
            new HttpClientImpl().request("http://localhost:" + port, HttpMethod.GET, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConnectException);
        }
    }

    @Test
    public void normalPath_registerHttpHandler_usePathHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);
        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandler(httpHandler, path);

        HttpClient httpClient = new HttpClientImpl();
        HttpResult result = httpClient.request("http://localhost:" + port + path, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 200);
        Assert.assertTrue(new String(result.getBody()).contains(path));

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normalPath_unregisterHttpHandler_noPathHttpService() throws Exception {
        int port = 7788;
        HttpServerImpl httpServer = build(port);

        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendFinish("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandler(httpHandler, path);
        httpServer.unregisterHttpHandler(httpHandler);
        HttpClient httpClient = new HttpClientImpl();
        HttpResult result = httpClient.request("http://localhost:" + port + path, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    static HttpServerImpl build(int port) throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", port + "");

        HttpServerImpl httpServer = new HttpServerImpl();
        Field loggerField = HttpServerImpl.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(httpServer, new LoggerImpl());

        httpServer.start(config);

        return httpServer;
    }
}
