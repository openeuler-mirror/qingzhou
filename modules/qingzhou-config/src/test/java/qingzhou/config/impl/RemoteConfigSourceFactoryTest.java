package qingzhou.config.impl;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.config.remote.RemoteConfigSource;
import qingzhou.config.remote.RemoteConfigSourceFactory;
import qingzhou.config.remote.etcd.EtcdConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;

/** 工厂的参数解析与必填校验；HttpClient / Json 以桩注入，不发起真实请求。 */
public class RemoteConfigSourceFactoryTest {
    private static final String ENDPOINT = "http://127.0.0.1:2379";

    @Test
    public void missingType_create_defaultsToEtcd() {
        Assert.assertTrue(create(validArgs()) instanceof EtcdConfigSource);
    }

    @Test
    public void blankType_create_defaultsToEtcd() {
        Assert.assertTrue(create(args("type", "  ", "namespace", "q/test", "endpoints", ENDPOINT)) instanceof EtcdConfigSource);
    }

    @Test
    public void upperCaseType_create_acceptedAsEtcd() {
        Assert.assertTrue(create(args("type", "ETCD", "namespace", "q/test", "endpoints", ENDPOINT)) instanceof EtcdConfigSource);
    }

    @Test
    public void unsupportedType_create_throwException() {
        try {
            create(args("type", "consul", "namespace", "q/test", "endpoints", ENDPOINT));
            Assert.fail("不支持的类型应抛出异常");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("consul"), e.getMessage());
        }
    }

    @Test
    public void missingEndpoints_create_throwException() {
        try {
            create(args("namespace", "q/test"));
            Assert.fail("缺少 endpoints 时应抛出异常");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(RemoteConfigSourceFactory.KEY_PREFIX + "endpoints"), e.getMessage());
        }
    }

    @Test
    public void blankEndpoints_create_throwException() {
        try {
            create(args("namespace", "q/test", "endpoints", "  "));
            Assert.fail("endpoints 为空白时应抛出异常");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(RemoteConfigSourceFactory.KEY_PREFIX + "endpoints"), e.getMessage());
        }
    }

    @Test
    public void nonNumericConnectTimeout_create_throwException() {
        try {
            create(args("namespace", "q/test", "endpoints", ENDPOINT, "connect_timeout", "abc"));
            Assert.fail("超时非数字时应抛出异常");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains(RemoteConfigSourceFactory.KEY_PREFIX + "connect_timeout"), e.getMessage());
        }
    }

    @Test
    public void nullHttpClient_create_throwException() {
        try {
            RemoteConfigSourceFactory.create(validArgs(), null, json());
            Assert.fail("HttpClient 为空时应抛出异常");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains("HttpClient"), e.getMessage());
        }
    }

    // ---------- 辅助 ----------

    private static RemoteConfigSource create(Map<String, String> args) {
        return RemoteConfigSourceFactory.create(args, http(), json());
    }

    private static Map<String, String> validArgs() {
        return args("namespace", "q/test", "endpoints", ENDPOINT);
    }

    private static Map<String, String> args(String... keyValues) {
        Map<String, String> args = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            args.put(RemoteConfigSourceFactory.KEY_PREFIX + keyValues[i], keyValues[i + 1]);
        }
        return args;
    }

    private static HttpClient http() {
        return TestSupport.proxy(HttpClient.class, (proxy, method, args) -> null);
    }

    private static Json json() {
        return TestSupport.proxy(Json.class, (proxy, method, args) -> null);
    }
}
