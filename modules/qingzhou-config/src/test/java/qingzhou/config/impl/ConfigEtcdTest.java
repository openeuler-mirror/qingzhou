package qingzhou.config.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.config.remote.etcd.EtcdConfigSource;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;

/** 端到端验证：开启 etcd 后 Config.init 拉取远程配置并写入 CM；HttpClient / Json 以桩注入。 */
public class ConfigEtcdTest {
    private static final String NS = "q/config-test";
    private static final String AUTH = "qingzhou-config.remote.username=mock-user\n"
            + "qingzhou-config.remote.password=mock-pass\n";

    @Test
    public void remoteHasKey_init_remoteOverridesLocalAndKeepsMissing() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200, "",
                "qingzhou-http-server.port=7900\nqingzhou-logger.writingthread=true\n",
                kv(NS + "/qingzhou-http-server", "port=9911\nhost=0.0.0.0\n"),
                kv(NS + "/qingzhou-logger", "level=info\n"),
                kv("q/other/qingzhou-logger", "writingthread=other\n"));// 其它命名空间的数据

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");// 远程覆盖本地同名 key
        Assert.assertEquals(updated.get("qingzhou-http-server").get("host"), "0.0.0.0");// 远程新增 key
        Assert.assertEquals(updated.get("qingzhou-logger").get("level"), "info");// 远程新增 pid
        Assert.assertEquals(updated.get("qingzhou-logger").get("writingthread"), "true");// 缺失保留本地，隔离生效
    }

    @Test
    public void selfBootstrapKey_init_notOverriddenByRemote() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200, "", "qingzhou-http-server.port=7900\n",
                kv(NS + "/qingzhou-config", "remote.enabled=false\n"));

        Assert.assertEquals(updated.get("qingzhou-config").get("remote.enabled"), "true");// 自举参数不被覆盖
    }

    @Test
    public void authConfigured_init_authenticatesThenPulls() throws Exception {
        Map<String, Dictionary<String, Object>> updated = runInit(200, AUTH, "qingzhou-http-server.port=7900\n",
                kv(NS + "/qingzhou-http-server", "port=9911\n"));

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");// 先鉴权再拉取
    }

    @Test
    public void httpError_init_throwsExceptionContainingStatus() {
        try {
            runInit(500, "", "qingzhou-http-server.port=7900\n");
            Assert.fail("远程返回 http 错误时应抛出异常");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("500"), "异常信息应包含状态码: " + e.getMessage());
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
        TestSupport.inject(config, "httpClient", http(status));
        TestSupport.inject(config, "json", json(range));
        config.init();
        return updated;
    }

    private static EtcdConfigSource.Kv kv(String key, String doc) {
        EtcdConfigSource.Kv kv = new EtcdConfigSource.Kv();
        kv.key = base64(key);
        kv.value = base64(doc);
        return kv;
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** 桩 HttpClient：newRequest 返回可链式调用的空对象，send 返回给定状态码与空响应体。 */
    private static HttpClient http(int status) {
        Request request = TestSupport.proxy(Request.class, (proxy, method, args) -> proxy);
        Response response = TestSupport.proxy(Response.class,
                (proxy, method, args) -> "getStatus".equals(method.getName()) ? status : new byte[0]);
        return TestSupport.proxy(HttpClient.class, (proxy, method, args) ->
                "newRequest".equals(method.getName()) ? request : response);
    }

    /** 桩 Json：鉴权请求返回固定 token，其余请求返回预置的 range 数据。 */
    private static Json json(EtcdConfigSource.Range range) {
        EtcdConfigSource.Auth auth = new EtcdConfigSource.Auth();
        auth.token = "mock-token";
        return TestSupport.proxy(Json.class, (proxy, method, args) -> "toJson".equals(method.getName())
                ? "{}"
                : ((Class<?>) args[1]).cast(EtcdConfigSource.Auth.class == args[1] ? auth : range));
    }
}
