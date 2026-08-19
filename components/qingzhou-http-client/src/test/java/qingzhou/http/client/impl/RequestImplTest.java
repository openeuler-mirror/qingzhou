package qingzhou.http.client.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.client.HttpMethod;

public class RequestImplTest {

    @Test
    public void validUrl_constructor_urlInitialized() {
        RequestImpl request = new RequestImpl("http://localhost:7900/registry/invoke");

        Assert.assertEquals(request.url, "http://localhost:7900/registry/invoke");
    }

    @Test
    public void validMethod_method_methodFieldSet() {
        RequestImpl request = new RequestImpl("http://localhost:7900");

        request.method(HttpMethod.POST);

        Assert.assertEquals(request.method, HttpMethod.POST);
    }

    @Test
    public void singleHeader_header_headersMapContainsEntry() {
        RequestImpl request = new RequestImpl("http://localhost:7900");

        request.header("X-Request-Id", "request-123");

        Assert.assertNotNull(request.headers);
        Assert.assertEquals(request.headers.size(), 1);
        Assert.assertEquals(request.headers.get("X-Request-Id"), "request-123");
    }

    @Test
    public void duplicateHeader_header_latestValueWins() {
        RequestImpl request = new RequestImpl("http://localhost:7900");

        request.header("X-Request-Id", "request-123");
        request.header("X-Request-Id", "request-456");

        Assert.assertEquals(request.headers.size(), 1);
        Assert.assertEquals(request.headers.get("X-Request-Id"), "request-456");
    }

    @Test
    public void bulkHeaders_headers_replacesExistingHeaders() {
        RequestImpl request = new RequestImpl("http://localhost:7900");
        request.header("Old-Header", "old-value");
        Map<String, String> newHeaders = new HashMap<>();
        newHeaders.put("New-Header", "new-value");

        request.headers(newHeaders);

        Assert.assertSame(request.headers, newHeaders);
        Assert.assertFalse(request.headers.containsKey("Old-Header"));
        Assert.assertEquals(request.headers.get("New-Header"), "new-value");
    }

    @Test
    public void paramsMap_params_paramsFieldStored() {
        RequestImpl request = new RequestImpl("http://localhost:7900");
        Map<String, String> params = new HashMap<>();
        params.put("name", "qingzhou");

        request.params(params);

        Assert.assertSame(request.params, params);
    }

    @Test
    public void byteArrayBody_body_bodyFieldStored() {
        RequestImpl request = new RequestImpl("http://localhost:7900");
        byte[] body = "name=qingzhou".getBytes(StandardCharsets.UTF_8);

        request.body(body);

        Assert.assertSame(request.body, body);
    }

    @Test
    public void filesMap_files_filesFieldStored() {
        RequestImpl request = new RequestImpl("http://localhost:7900");
        Map<String, String> files = new HashMap<>();
        files.put("upload", "/tmp/upload.txt");

        request.files(files);

        Assert.assertSame(request.files, files);
    }

    @Test
    public void positiveValue_connectTimeout_timeoutFieldSet() {
        RequestImpl request = new RequestImpl("http://localhost:7900");

        request.connectTimeout(5000);

        Assert.assertEquals(request.connectTimeout, 5000);
    }

    @Test
    public void positiveValue_readTimeout_timeoutFieldSet() {
        RequestImpl request = new RequestImpl("http://localhost:7900");

        request.readTimeout(10000);

        Assert.assertEquals(request.readTimeout, 10000);
    }

    @Test
    public void chainedCalls_setters_returnSameInstance() {
        RequestImpl request = new RequestImpl("http://localhost:7900");
        Map<String, String> headers = new HashMap<>();
        Map<String, String> params = new HashMap<>();
        Map<String, String> files = new HashMap<>();

        Assert.assertSame(request.method(HttpMethod.GET), request);
        Assert.assertSame(request.header("X-Request-Id", "request-123"), request);
        Assert.assertSame(request.headers(headers), request);
        Assert.assertSame(request.params(params), request);
        Assert.assertSame(request.body(new byte[0]), request);
        Assert.assertSame(request.files(files), request);
        Assert.assertSame(request.connectTimeout(3000), request);
        Assert.assertSame(request.readTimeout(6000), request);

        Assert.assertSame(request.method(HttpMethod.POST)
                .header("X-Request-Id", "request-456")
                .headers(headers)
                .params(params)
                .body(new byte[0])
                .files(files)
                .connectTimeout(3000)
                .readTimeout(6000), request);
    }
}
