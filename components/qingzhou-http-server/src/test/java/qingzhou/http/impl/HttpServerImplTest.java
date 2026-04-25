package qingzhou.http.impl;

import java.lang.reflect.Field;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpHandler;

import static qingzhou.http.impl.HttpServiceEngineTest.build;

public class HttpServerImplTest {

    @Test
    public void normalPath_registerHttpHandler_usePathHttpService() throws Exception {
        int port = 7788;
        HttpServiceEngine serviceEngine = build(port);
        HttpServerImpl httpServer = buildHttpServer(serviceEngine);
        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendResponse("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandler(httpHandler, path);

        HttpClient httpClient = new HttpClientImpl();
        HttpResult result = httpClient.request("http://localhost:" + port + path, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 200);
        Assert.assertTrue(new String(result.getBody()).contains(path));

        serviceEngine.stop(); // 清理资源
    }

    @Test
    public void normalPath_unregisterHttpHandler_noPathHttpService() throws Exception {
        int port = 7788;
        HttpServiceEngine serviceEngine = build(port);
        HttpServerImpl httpServer = buildHttpServer(serviceEngine);

        String path = "/testHttp";
        HttpHandler httpHandler = (httpRequest, httpResponse) -> httpResponse.sendResponse("Hello: " + httpRequest.getPath());
        httpServer.registerHttpHandler(httpHandler, path);
        httpServer.unregisterHttpHandler(httpHandler);
        HttpClient httpClient = new HttpClientImpl();
        HttpResult result = httpClient.request("http://localhost:" + port + path, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 404);

        serviceEngine.stop(); // 清理资源
    }

    private HttpServerImpl buildHttpServer(HttpServiceEngine serviceEngine) throws Exception {
        HttpServerImpl httpServer = new HttpServerImpl();
        Field httpServiceEngineField = HttpServerImpl.class.getDeclaredField("httpServiceEngine");
        httpServiceEngineField.setAccessible(true);
        httpServiceEngineField.set(httpServer, serviceEngine);
        return httpServer;
    }
}
