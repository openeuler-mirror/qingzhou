package qingzhou.http.impl;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.netty.http.server.HttpServerRequest;

/**
 * HttpRequestImpl 自动化测试集。
 * <p>
 * HttpRequestImpl 是服务端收到请求后的内部包装类，将 reactor-netty 的 HttpServerRequest
 * 适配为框架统一的 HttpRequest。本测试不依赖真实网络，通过动态代理构造 HttpServerRequest
 * 桩对象，直接验证 HttpRequestImpl 的解析与转发行为，重点锁定 getParameters() 的
 * query 与表单体合并算法、setRequestBody() 后的缓存失效等无端到端覆盖的逻辑。
 */
public class HttpRequestImplTest {

    // ---------- 1. URL QueryString 解析 ----------

    @Test
    public void uriWithQuery_getParameters_returnsAllParams() {
        HttpRequestImpl request = newRequest("/path?a=1&b=2", "/path", HttpMethod.GET, null, false);

        Map<String, java.util.List<String>> parameters = request.getParameters();

        Assert.assertNotNull(parameters);
        Assert.assertEquals(parameters.size(), 2);
        Assert.assertEquals(parameters.get("a"), Arrays.asList("1"));
        Assert.assertEquals(parameters.get("b"), Arrays.asList("2"));
    }

    // ---------- 2. query 与表单体合并 ----------

    @Test
    public void queryAndFormBody_sameNameParam_queryValuesBeforeBodyValues() {
        HttpRequestImpl request = newRequest("/path?name=queryName&tag=q1", "/path", HttpMethod.POST, formHeaders(), true);
        request.setRequestBody("name=bodyName&extra=be".getBytes(StandardCharsets.UTF_8));

        Map<String, java.util.List<String>> parameters = request.getParameters();

        // 同名参数按“query 在前、body 在后”顺序合并
        Assert.assertEquals(parameters.get("name"), Arrays.asList("queryName", "bodyName"));
        Assert.assertEquals(parameters.get("tag"), Arrays.asList("q1"));
        Assert.assertEquals(parameters.get("extra"), Arrays.asList("be"));
        // query 独有的 name 取第一个时仍为 query 值
        Assert.assertEquals(request.getParameter("name"), "queryName");
    }

    // ---------- 3. 非表单请求体不参与解析 ----------

    @Test
    public void nonFormBody_getParameters_ignoresBodyParams() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.POST, nonFormHeaders(), false);
        request.setRequestBody("name=bodyOnly".getBytes(StandardCharsets.UTF_8));

        Map<String, java.util.List<String>> parameters = request.getParameters();

        Assert.assertTrue(parameters.isEmpty());
        Assert.assertNull(request.getParameter("name"));
    }

    // ---------- 3.1 表单类型但请求体是二进制密文 ----------

    @Test
    public void binaryBodyWithFormType_getParameters_keepsQueryParams() {
        HttpRequestImpl request = newRequest("/path?key=dup", "/path", HttpMethod.POST, formHeaders(), true);
        request.setRequestBody("ab%zz=1".getBytes(StandardCharsets.UTF_8));

        Map<String, java.util.List<String>> parameters = request.getParameters();

        Assert.assertEquals(parameters.get("key"), Arrays.asList("dup"));
        Assert.assertEquals(request.getParameter("key"), "dup");
    }

    // ---------- 4. 无 query 无 body 返回空集合 ----------

    @Test
    public void noQueryNoBody_getParameters_returnsEmptyMapNotNul() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET, null, false);

        Map<String, java.util.List<String>> parameters = request.getParameters();

        Assert.assertNotNull(parameters);
        Assert.assertTrue(parameters.isEmpty());
    }

    // ---------- 5. 单值读取 ----------

    @Test
    public void multiValueParam_getParameter_returnsFirstValue() {
        HttpRequestImpl request = newRequest("/path?k=v1&k=v2", "/path", HttpMethod.GET, null, false);

        Assert.assertEquals(request.getParameter("k"), "v1");
    }

    @Test
    public void absentParam_getParameter_returnsNull() {
        HttpRequestImpl request = newRequest("/path?a=1", "/path", HttpMethod.GET, null, false);

        Assert.assertNull(request.getParameter("missing"));
    }

    // ---------- 6. 缓存失效 ----------

    @Test
    public void bodyUpdated_getParameters_reparsesFromNewBody() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.POST, formHeaders(), true);

        request.setRequestBody("key=oldValue&extra=1".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(request.getParameters().get("key"), Arrays.asList("oldValue"));

        // 更新 body 后再次解析，必须按新 body 重建，而非命中旧缓存
        request.setRequestBody("key=newValue".getBytes(StandardCharsets.UTF_8));
        Map<String, java.util.List<String>> parameters = request.getParameters();

        Assert.assertEquals(parameters.get("key"), Arrays.asList("newValue"));
        Assert.assertFalse(parameters.containsKey("extra"));
    }

    // ---------- 7. 请求行读取 ----------

    @Test
    public void requestLinePresent_getMethodPathFullPath_returnCorrectValues() {
        HttpRequestImpl request = newRequest("/api/user/1?debug=1&page=2", "/api/user/1", HttpMethod.DELETE, null, false);

        Assert.assertEquals(request.getMethod(), "DELETE");
        Assert.assertEquals(request.getPath(), "/api/user/1");
        Assert.assertEquals(request.getFullPath(), "/api/user/1?debug=1&page=2");
    }

    // ---------- 8. 请求头读取 ----------

    @Test
    public void headersSet_getHeaderContentTypeForm_returnConfiguredValues() {
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("X-Trace-Id", "trace-123");
        headers.add(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.POST, headers, true);

        Assert.assertEquals(request.getHeader("X-Trace-Id"), "trace-123");
        Assert.assertEquals(request.getContentType(), "application/x-www-form-urlencoded");
        Assert.assertTrue(request.isFormUrlencoded());
    }

    // ---------- 9. 远端地址 ----------

    @Test
    public void remoteAddressPresent_getRemoteHost_returnsHostString() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET,
                null, false, new InetSocketAddress("127.0.0.1", 8080));

        Assert.assertEquals(request.getRemoteHost(), "127.0.0.1");
    }

    @Test
    public void nullRemoteAddress_getRemoteHost_returnsUnknown() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET, null, false, null);

        Assert.assertEquals(request.getRemoteHost(), "unknown");
    }

    // ---------- 10. 属性存取 ----------

    @Test
    public void attributeSet_getAttribute_returnsStoredValue() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET, null, false);
        Object attribute = new Object();

        request.setAttribute("user", attribute);

        Assert.assertSame(request.getAttribute("user"), attribute);
    }

    @Test
    public void attributeNotSet_getAttribute_returnsNull() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET, null, false);

        Assert.assertNull(request.getAttribute("never-set"));
    }

    // ---------- 11. 请求体读取 ----------

    @Test
    public void bodyNotSet_getBody_returnsNull() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.GET, null, false);

        Assert.assertNull(request.getBody());
    }

    @Test
    public void bodyUpdated_getBody_returnsLatestBytes() {
        HttpRequestImpl request = newRequest("/path", "/path", HttpMethod.POST, null, false);
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        byte[] second = "second".getBytes(StandardCharsets.UTF_8);

        request.setRequestBody(first);
        Assert.assertSame(request.getBody(), first);

        request.setRequestBody(second);
        Assert.assertSame(request.getBody(), second);
    }

    // ---------- 桩对象构造 ----------

    /** 默认桩：无请求头、远端地址为 null、非表单类型。 */
    private HttpRequestImpl newRequest(String uri, String path, HttpMethod method, HttpHeaders headers, boolean form) {
        return newRequest(uri, path, method, headers, form, null);
    }

    /** 以动态代理构造 HttpServerRequest 桩，模拟任务书所需的服务端请求场景。 */
    private HttpRequestImpl newRequest(String uri, String path, HttpMethod method,
                                       HttpHeaders headers, boolean form, SocketAddress remoteAddress) {
        HttpServerRequest stub = (HttpServerRequest) Proxy.newProxyInstance(
                HttpServerRequest.class.getClassLoader(),
                new Class<?>[]{HttpServerRequest.class},
                (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "uri":
                            return uri;
                        case "method":
                            return method;
                        case "remoteAddress":
                            return remoteAddress;
                        case "requestHeaders":
                            return headers != null ? headers : new DefaultHttpHeaders();
                        case "isFormUrlencoded":
                            return form;
                        default:
                            return primitiveDefault(m.getReturnType());
                    }
                });
        return new HttpRequestImpl(stub, path);
    }

    /** 为代理桩中未显式配置的原始类型方法提供默认值，避免反射调用抛 NPE。 */
    private Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        return headers;
    }

    private HttpHeaders nonFormHeaders() {
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return headers;
    }
}
