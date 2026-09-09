package qingzhou.config.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 端到端验证：qingzhou.properties 开启 etcd 后，Config.init 从模拟的 etcd
 * v3 JSON Gateway 拉取配置，按“远程覆盖本地、缺失保留”合并后写入 CM。
 */
public class ConfigEtcdTest {
    private static final String NS = "q/config-test";

    @Test
    public void remoteHasKey_localMerge_remoteWins() throws Exception {
        String remoteServer = startServer(200,
                "{\"kvs\":["
                        + kv(NS + "/qingzhou-http-server", "port=9911\nhost=0.0.0.0\n") + ","
                        + kv(NS + "/qingzhou-logger", "writingthread=false\n") + ","
                        + kv(NS + "/qingzhou-registry", "interval=9\n") + ","
                        + kv("q/other/qingzhou-logger", "writingthread=other-namespace\n") + ","
                        + kv(NS + "/qingzhou-config", "remote.enabled=false\n")
                        + "],\"count\":5}");

        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        runInit(updated, remoteServer, "qingzhou-http-server.port=7900\n"
                + "qingzhou-logger.writingthread=true\n");

        Dictionary<String, Object> http = updated.get("qingzhou-http-server");
        Assert.assertNotNull(http);
        Assert.assertEquals(http.get("port"), "9911"); // 远程覆盖本地
        Assert.assertEquals(http.get("host"), "0.0.0.0"); // 远程新增 key

        Assert.assertEquals(updated.get("qingzhou-logger").get("writingthread"), "false");

        Assert.assertEquals(updated.get("qingzhou-registry").get("interval"), "9"); // 远程新增 pid

        Dictionary<String, Object> self = updated.get("qingzhou-config");
        Assert.assertNotNull(self);
        Assert.assertEquals(self.get("remote.enabled"), "true"); // 自举参数不被远程覆盖
    }

    @Test
    public void remoteMissKey_localMerge_localKept() throws Exception {
        String remoteServer = startServer(200,
                "{\"kvs\":[" + kv(NS + "/qingzhou-http-server", "port=9911\n") + "],\"count\":1}");

        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        runInit(updated, remoteServer, "qingzhou-http-server.port=7900\n"
                + "qingzhou-logger.writingthread=true\n");

        Dictionary<String, Object> logger = updated.get("qingzhou-logger");
        Assert.assertNotNull(logger);
        Assert.assertEquals(logger.get("writingthread"), "true"); // 远程缺失，保留本地
    }

    @Test
    public void emptyRemoteConfig_localMerge_noChange() throws Exception {
        String remoteServer = startServer(200, "{\"kvs\":[],\"count\":0}");

        Map<String, Dictionary<String, Object>> updated = new HashMap<>();
        runInit(updated, remoteServer, "qingzhou-http-server.port=7900\n");

        Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "7900");
    }

    @Test
    public void authEnabled_pull_authenticatesAndMerges() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/auth/authenticate", exchange -> {
            String body = readAll(exchange.getRequestBody());
            Assert.assertTrue(body.contains("mock-user"));
            Assert.assertTrue(body.contains("mock-pass"));
            respond(exchange, 200, "{\"token\":\"mock-token\"}");
        });
        server.createContext("/v3/kv/range", exchange -> {
            Assert.assertEquals(exchange.getRequestHeaders().getFirst("Authorization"), "mock-token");
            respond(exchange, 200, "{\"kvs\":[" + kv(NS + "/qingzhou-http-server", "port=9911\n") + "],\"count\":1}");
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();

            Map<String, Dictionary<String, Object>> updated = new HashMap<>();
            runInit(updated, base, "qingzhou-http-server.port=7900\n",
                    "mock-user", "mock-pass");

            Assert.assertEquals(updated.get("qingzhou-http-server").get("port"), "9911");
        } finally {
            server.stop(0);
        }
    }

    // ---------- 辅助 ----------

    private static String kv(String key, String value) {
        return "{\"key\":\"" + b64(key) + "\",\"value\":\"" + b64(value) + "\"}";
    }

    private static String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String startServer(int status, String json) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/kv/range", exchange -> {
            readAll(exchange.getRequestBody());
            respond(exchange, status, json);
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        return base;
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            for (int n; (n = is.read(buffer)) != -1; ) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 便捷入口：不启用 etcd 鉴权。
     */
    private void runInit(Map<String, Dictionary<String, Object>> updated, String endpoints, String localConfig) throws Exception {
        runInit(updated, endpoints, localConfig, null, null);
    }

    private void runInit(Map<String, Dictionary<String, Object>> updated, String endpoints, String localConfig,
                         String username, String password) throws Exception {
        StringBuilder content = new StringBuilder();
        content.append("qingzhou-config.remote.enabled=true\n");
        content.append("qingzhou-config.remote.type=etcd\n");
        content.append("qingzhou-config.remote.endpoints=").append(endpoints).append('\n');
        content.append("qingzhou-config.remote.namespace=").append(NS).append('\n');
        content.append("qingzhou-config.remote.connect_timeout=5\n");
        content.append("qingzhou-config.remote.read_timeout=5\n");
        if (username != null) {
            content.append("qingzhou-config.remote.username=").append(username).append('\n');
            content.append("qingzhou-config.remote.password=").append(password).append('\n');
        }
        content.append(localConfig);

        Path instanceDir = Files.createTempDirectory("qingzhou-instance-etcd");
        try {
            Files.createDirectories(instanceDir.resolve("conf"));
            Files.write(instanceDir.resolve("conf/qingzhou.properties"), content.toString().getBytes(StandardCharsets.UTF_8));
            System.setProperty("qingzhou.instance", instanceDir.toString());

            Config config = new Config();
            setField(config, "configAdmin", newConfigAdminStub(updated));
            config.init();
        } finally {
            deleteRecursively(instanceDir.toFile());
        }
    }

    private ConfigurationAdmin newConfigAdminStub(Map<String, Dictionary<String, Object>> updated) {
        return (ConfigurationAdmin) Proxy.newProxyInstance(
                ConfigEtcdTest.class.getClassLoader(),
                new Class<?>[]{ConfigurationAdmin.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getConfiguration".equals(name)) {
                        return recordingConfiguration((String) args[0], updated);
                    }
                    if ("getFactoryConfiguration".equals(name)) {
                        return recordingConfiguration(args[0] + "~" + args[1], updated);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    private Configuration recordingConfiguration(String pid, Map<String, Dictionary<String, Object>> updated) {
        return (Configuration) Proxy.newProxyInstance(
                ConfigEtcdTest.class.getClassLoader(),
                new Class<?>[]{Configuration.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("update".equals(name)) {
                        updated.put(pid, (Dictionary<String, Object>) args[0]);
                        return null;
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteRecursively(java.io.File file) {
        if (file == null || !file.exists()) return;
        java.io.File[] children = file.listFiles();
        if (children != null) {
            for (java.io.File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
