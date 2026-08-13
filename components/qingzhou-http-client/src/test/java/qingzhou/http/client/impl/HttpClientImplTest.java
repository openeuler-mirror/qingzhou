package qingzhou.http.client.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;

public class HttpClientImplTest {
    @Test
    public void normal_send_getResponse() throws Exception {
        HttpClient httpClient = new HttpClientImpl();
        Request request = httpClient.newRequest("https://www.baidu.com/");
        Response response = httpClient.send(request);
        String text = new String(response.getBody(), StandardCharsets.UTF_8);
        Assert.assertNotNull(text);
        Assert.assertTrue(text.contains("百度"));
    }

    @Test
    public void normal_sendWithResponseListener_getResponse() throws Exception {
        List<String> lines = new ArrayList<>();
        final boolean[] onComplete = {false};
        final boolean[] onError = {false};

        HttpClient httpClient = new HttpClientImpl();
        Request request = httpClient.newRequest("https://www.baidu.com/");
        httpClient.send(request, new ResponseListener() {
            @Override
            public void onBody(String line) {
                lines.add(line);
            }

            @Override
            public void onComplete() {
                onComplete[0] = true;
            }

            @Override
            public void onError(Throwable t) {
                onError[0] = true;
            }
        });

        Assert.assertTrue(lines.isEmpty());
        Assert.assertFalse(onComplete[0]);
        Assert.assertFalse(onError[0]);

        Thread.sleep(5000);

        Assert.assertFalse(lines.isEmpty());
        Assert.assertTrue(onComplete[0]);
        Assert.assertFalse(onError[0]);
    }
}
