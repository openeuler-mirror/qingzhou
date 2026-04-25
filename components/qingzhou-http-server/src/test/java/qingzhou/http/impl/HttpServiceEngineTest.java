package qingzhou.http.impl;

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.logger.impl.LoggerImpl;

public class HttpServiceEngineTest {
    @Test
    public void normal_start_listenHttpService() throws Exception {
        int port = 7788;
        HttpServiceEngine httpServer = build(port);
        HttpResult result = new HttpClientImpl().request("http://localhost:" + port, HttpMethod.GET, null);
        Assert.assertEquals(result.getStatus(), 404);

        httpServer.stop(); // 清理资源
    }

    @Test
    public void normal_stop_requestGetConnectException() throws Exception {
        int port = 7788;
        HttpServiceEngine httpServer = build(port);
        httpServer.stop();
        try {
            new HttpClientImpl().request("http://localhost:" + port, HttpMethod.GET, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConnectException);
        }
    }

    static HttpServiceEngine build(int port) throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("port", port + "");

        HttpServiceEngine serviceEngine = new HttpServiceEngine();
        Field loggerField = HttpServiceEngine.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(serviceEngine, new LoggerImpl());

        serviceEngine.start(config);

        return serviceEngine;
    }
}
