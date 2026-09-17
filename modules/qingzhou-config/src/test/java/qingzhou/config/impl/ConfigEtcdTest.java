package qingzhou.config.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qingzhou.config.remote.etcd.EtcdConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;

/**
 * 端到端验证：开启 etcd 后 Config.init 拉取远程配置并写入 CM；HttpClient / Json 以桩注入。
 */
public class ConfigEtcdTest {
    private static final String NS = "q/config-test";

    @BeforeClass
    public void init() {
        System.setProperty("qingzhou.instance", new File("/tmp").getAbsolutePath());
        System.setProperty("qingzhou.version", "1.0");
    }

    @Test
    public void remoteHasKey_init_remoteOverridesLocalAndKeepsMissing() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200, "",
                "qingzhou-http-server.port=7900\nqingzhou-logger.writingthread=true\n",
                kv(NS + "/qingzhou-http-server/port", "9911"),
                kv(NS + "/qingzhou-http-server/host", "0.0.0.0"),
                kv(NS + "/qingzhou-logger/level", "info"),
                kv("q/other/qingzhou-logger/writingthread", "other"));// 其它命名空间的数据

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");// 远程覆盖本地同名 key
        Assert.assertEquals(updated.get("qingzhou-http-server").get("host"), "0.0.0.0");// 远程新增 key
        Assert.assertEquals(updated.get("qingzhou-logger").get("level"), "info");// 远程新增 pid
        Assert.assertEquals(updated.get("qingzhou-logger").get("writingthread"), "true");// 缺失保留本地，隔离生效
    }

    @Test
    public void selfBootstrapKey_init_notOverriddenByRemote() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200, "", "qingzhou-http-server.port=7900\n",
                kv(NS + "/qingzhou-http-server/port", "9911"),// 远端非自举键，用于排除空拉取
                kv(NS + "/qingzhou-config/remote.enabled", "false"));

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");// 远端数据确实已生效
        Assert.assertEquals(updated.get("qingzhou-config").get("remote.enabled"), "true");// 自举参数不被覆盖
    }

    @Test
    public void authConfigured_init_authenticatesThenPulls() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200,
                "qingzhou-config.remote.username=mock-user\nqingzhou-config.remote.password=mock-pass\n",
                "qingzhou-http-server.port=7900\n", kv(NS + "/qingzhou-http-server/port", "9911"));

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");// 先鉴权再拉取
    }

    @Test
    public void httpError_pull_throwsExceptionContainingStatus() {
        EtcdConfigSource source = new EtcdConfigSource("http://127.0.0.1:2379", NS, null, null, 1, 1,
                http(500, null), json(new EtcdConfigSource.Range()));
        try {
            source.pull();
            Assert.fail("远程返回 http 错误时应抛出异常");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("500"), "异常信息应包含状态码: " + e.getMessage());
        }
    }

    @Test
    public void endpointWithV3Suffix_pull_requestsNormalizedPath() throws Exception {
        List<String> urls = new ArrayList<>();
        EtcdConfigSource source = new EtcdConfigSource("http://127.0.0.1:2379/v3/", NS,
                null, null, 1, 1, http(200, urls), json(new EtcdConfigSource.Range()));
        source.pull();

        Assert.assertEquals(urls.get(0), "http://127.0.0.1:2379/v3/kv/range");// 尾斜杠与 /v3 后缀已归一化
    }

    @Test
    public void etcdResponseDtos_fields_matchEtcdProtocol() throws Exception {
        Assert.assertEquals(EtcdConfigSource.Range.class.getField("kvs").getType(), List.class);
        Assert.assertEquals(EtcdConfigSource.Kv.class.getField("key").getType(), String.class);
        Assert.assertEquals(EtcdConfigSource.Kv.class.getField("value").getType(), String.class);
        Assert.assertEquals(EtcdConfigSource.Auth.class.getField("token").getType(), String.class);// 字段名即协议字段名
    }

    @Test
    public void emptyNamespace_construct_throwsException() {
        try {
            new EtcdConfigSource("http://127.0.0.1:2379", "", null, null, 1, 1, null, null);
            Assert.fail("命名空间为空时应抛出异常");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("namespace"));
        }
    }

    private static Map<String, Dictionary<String, Object>> runInit(int status, String auth, String local,
                                                                   EtcdConfigSource.Kv... kvs) throws Exception {
        TestSupport.instance("qingzhou-config.remote.enabled=true\n"
                + "qingzhou-config.remote.endpoints=http://127.0.0.1:2379\n"
                + "qingzhou-config.remote.namespace=" + NS + "\n"
                + "qingzhou-config.remote.connect_timeout=1\n"
                + "qingzhou-config.remote.read_timeout=1\n" + auth + local);

        EtcdConfigSource.Range range = new EtcdConfigSource.Range();
        range.kvs = new ArrayList<>();
        for (EtcdConfigSource.Kv kv : kvs) range.kvs.add(kv);

        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        Config config = new Config();
        TestSupport.inject(config, "configAdmin", TestSupport.admin(updated));
        TestSupport.inject(config, "httpClient", http(status, null));
        TestSupport.inject(config, "json", json(range));
        config.init();
        return updated;
    }

    /**
     * etcd 键布局为 namespace/pid/配置项，值为该配置项的原始值；归一化后即 pid.配置项。
     */
    private static EtcdConfigSource.Kv kv(String key, String value) {
        EtcdConfigSource.Kv kv = new EtcdConfigSource.Kv();
        kv.key = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        kv.value = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return kv;
    }

    /**
     * 桩 HttpClient：newRequest 返回可链式调用的空对象（可记录 URL），send 返回给定状态码与空响应体。
     */
    private static HttpClient http(int status, List<String> urls) {
        Request request = TestSupport.proxy(Request.class, (proxy, method, args) -> proxy);
        Response response = TestSupport.proxy(Response.class,
                (proxy, method, args) -> "getStatus".equals(method.getName()) ? status : new byte[0]);
        return TestSupport.proxy(HttpClient.class, (proxy, method, args) -> {
            if (!"newRequest".equals(method.getName())) return response;
            if (urls != null) urls.add((String) args[0]);
            return request;
        });
    }

    /**
     * 桩 Json：鉴权请求返回固定 token，其余请求返回预置的 range 数据。
     */
    private static Json json(EtcdConfigSource.Range range) {
        EtcdConfigSource.Auth auth = new EtcdConfigSource.Auth();
        auth.token = "mock-token";
        return TestSupport.proxy(Json.class, (proxy, method, args) -> "toJson".equals(method.getName())
                ? "{}"
                : ((Class<?>) args[1]).cast(EtcdConfigSource.Auth.class == args[1] ? auth : range));
    }
}
