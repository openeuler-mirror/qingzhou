package qingzhou.agent;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.api.AppContext;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.impl.HttpServerImpl;
import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.impl.LoggerImpl;
import qingzhou.registry.AppStub;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.AppStubRemote;
import qingzhou.registry.Registry;
import reactor.netty.DisposableServer;

/**
 * Agent 自动化测试集：配合 HttpServer 各 API、真实 Crypto/Json 组件端到端验证代理行为。
 */
public class AgentTest {

    @Test
    public void encryptedRequest_send_returnsEncryptedResponse() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            StubRegistry registry = new StubRegistry();
            StubAppStub appStub = new StubAppStub();
            registry.apps.put("demo", appStub);

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, registry), "/agent");

            RequestImpl request = new RequestImpl();
            request.setApp("demo");
            request.setModel("dashboard");
            request.setAction("monitor");
            JsonImpl json = new JsonImpl();
            json.init();
            byte[] body = crypto.getCipher(key).encrypt(json.toJson(request).getBytes(StandardCharsets.UTF_8));

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 200);
            String decrypted = new String(crypto.getCipher(key).decrypt(result.getBody()), StandardCharsets.UTF_8);
            Map<String, Object> responseMap = json.fromJson(decrypted, Map.class);
            Assert.assertEquals(responseMap.get("success"), true);
            Assert.assertEquals(responseMap.get("data"), "stub-data");
            Assert.assertNotNull(appStub.invoked.get());
            Assert.assertEquals(appStub.invoked.get().getApp(), "demo");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void wrongKeyRequest_send_returns500KeyAuthError() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(crypto.generateKey());

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            byte[] body = crypto.getCipher(crypto.generateKey())
                    .encrypt("tampered".getBytes(StandardCharsets.UTF_8)); // 用另一把密钥加密

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 500);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "key auth error");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void emptyBodyRequest_send_requestIgnored() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            Heartbeat.thisInstanceInfo = buildInstanceInfo(new CryptoImpl().generateKey());

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.GET));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(result.getBody().length, 0);
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void unregisteredAgent_request_requestIgnored() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            Heartbeat.thisInstanceInfo = null; // 代理尚未注册

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            CryptoImpl crypto = new CryptoImpl();
            byte[] body = crypto.getCipher(crypto.generateKey())
                    .encrypt("whatever".getBytes(StandardCharsets.UTF_8));
            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertEquals(result.getBody().length, 0);
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void unknownAppRequest_send_returns500BusinessError() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            RequestImpl request = new RequestImpl();
            request.setApp("ghost"); // 未注册的应用
            JsonImpl json = new JsonImpl();
            json.init();
            byte[] body = crypto.getCipher(key).encrypt(json.toJson(request).getBytes(StandardCharsets.UTF_8));

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 500);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "business processing error");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void invokeThrowsRequest_send_returns500BusinessError() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            StubRegistry registry = new StubRegistry();
            registry.apps.put("demo", new StubAppStub(true)); // invokeApp 抛异常
            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, registry), "/agent");

            RequestImpl request = new RequestImpl();
            request.setApp("demo");
            JsonImpl json = new JsonImpl();
            json.init();
            byte[] body = crypto.getCipher(key).encrypt(json.toJson(request).getBytes(StandardCharsets.UTF_8));

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 500);
            Assert.assertEquals(new String(result.getBody(), StandardCharsets.UTF_8), "business processing error");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void withKeyUpload_send_fileWrittenWithContent() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            byte[] plain = "hello-upload-data".getBytes(StandardCharsets.UTF_8);
            byte[] body = crypto.getCipher(key).encrypt(plain);
            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent/upload?key=mykey")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 200);
            String decrypted = new String(crypto.getCipher(key).decrypt(result.getBody()), StandardCharsets.UTF_8);
            Assert.assertEquals(decrypted, "mykey");
            byte[] written = Files.readAllBytes(new File(uploadBase, "mykey").toPath());
            Assert.assertEquals(new String(written, StandardCharsets.UTF_8), "hello-upload-data");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void withoutKeyUpload_send_uploadIdGeneratedAndWritten() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            byte[] plain = "no-key-data".getBytes(StandardCharsets.UTF_8);
            byte[] body = crypto.getCipher(key).encrypt(plain);
            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent/upload")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 200);
            String uploadId = new String(crypto.getCipher(key).decrypt(result.getBody()), StandardCharsets.UTF_8);
            Assert.assertEquals(uploadId.length(), 36); // UUID 长度
            File[] files = uploadBase.listFiles();
            Assert.assertNotNull(files);
            Assert.assertEquals(files.length, 1);
            Assert.assertEquals(new String(Files.readAllBytes(files[0].toPath()), StandardCharsets.UTF_8), "no-key-data");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void duplicateKeyUpload_send_dataAppended() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, new StubRegistry()), "/agent");

            HttpClient client = new HttpClientImpl();
            String url = "http://localhost:" + testServer.port + "/agent/upload?key=dup";
            Response first = client.send(client.newRequest(url)
                    .method(HttpMethod.POST)
                    .body(crypto.getCipher(key).encrypt("part1-".getBytes(StandardCharsets.UTF_8))));
            Response second = client.send(client.newRequest(url)
                    .method(HttpMethod.POST)
                    .body(crypto.getCipher(key).encrypt("part2-".getBytes(StandardCharsets.UTF_8))));

            Assert.assertEquals(first.getStatus(), 200);
            Assert.assertEquals(second.getStatus(), 200);
            Assert.assertEquals(new String(Files.readAllBytes(new File(uploadBase, "dup").toPath()), StandardCharsets.UTF_8),
                    "part1-part2-");
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    @Test
    public void forwardedRequest_send_tempFilesCleaned() throws Exception {
        File uploadBase = Files.createTempDirectory("agent-upload-").toFile();
        TestServer testServer = startServer();
        try {
            CryptoImpl crypto = new CryptoImpl();
            String key = crypto.generateKey();
            Heartbeat.thisInstanceInfo = buildInstanceInfo(key);

            // 模拟先前经 /upload 上传的临时文件
            Files.write(new File(uploadBase, "tmpkey").toPath(), "file-bytes".getBytes(StandardCharsets.UTF_8));

            StubRegistry registry = new StubRegistry();
            StubAppStub appStub = new StubAppStub();
            registry.apps.put("demo", appStub);
            testServer.server.registerHttpHandlerNoAuth(buildAgent(uploadBase, registry), "/agent");

            RequestImpl request = new RequestImpl();
            request.setApp("demo");
            request.getParameters().put("upload", "tmpkey=localfile.txt");
            request.getUploadFileFields().add("upload");
            JsonImpl json = new JsonImpl();
            json.init();
            byte[] body = crypto.getCipher(key).encrypt(json.toJson(request).getBytes(StandardCharsets.UTF_8));

            HttpClient client = new HttpClientImpl();
            Response result = client.send(client.newRequest("http://localhost:" + testServer.port + "/agent")
                    .method(HttpMethod.POST)
                    .body(body));

            Assert.assertEquals(result.getStatus(), 200);
            Assert.assertNotNull(appStub.invoked.get());
            // 转发给应用时参数已改名为本地绝对路径
            Assert.assertEquals(appStub.invoked.get().getParameter("upload"),
                    new File(uploadBase, "localfile.txt").getAbsolutePath());
            // 处理完成后临时文件被清理
            File[] files = uploadBase.listFiles();
            Assert.assertNotNull(files);
            Assert.assertEquals(files.length, 0);
        } finally {
            Heartbeat.thisInstanceInfo = null;
            testServer.server.stop();
            deleteRecursively(uploadBase);
        }
    }

    // ---------- 辅助方法 ----------

    private Agent buildAgent(File uploadBase, Registry registry) throws Exception {
        Agent agent = new Agent();
        setField(agent, "json", newJson());
        setField(agent, "crypto", new CryptoImpl());
        setField(agent, "registry", registry);
        setField(agent, "uploadBase", uploadBase);
        return agent;
    }

    private JsonImpl newJson() {
        JsonImpl json = new JsonImpl();
        json.init();
        return json;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private InstanceInfo buildInstanceInfo(String key) {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setKey(key);
        instanceInfo.setId("test-instance");
        return instanceInfo;
    }

    private TestServer startServer() throws Exception {
        HttpServerImpl httpServer = new HttpServerImpl();
        setField(httpServer, "logger", new LoggerImpl());
        Map<String, String> config = new HashMap<>();
        config.put("port", "0"); // 由操作系统分配空闲端口
        config.put("ssl_enabled", "false"); // 由操作系统分配空闲端口
        httpServer.start(config);
        Field field = HttpServerImpl.class.getDeclaredField("disposableServer");
        field.setAccessible(true);
        DisposableServer disposableServer = (DisposableServer) field.get(httpServer);
        java.net.InetSocketAddress address = (java.net.InetSocketAddress) disposableServer.address();
        return new TestServer(httpServer, address.getPort());
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static class TestServer {
        final HttpServerImpl server;
        final int port;

        TestServer(HttpServerImpl server, int port) {
            this.server = server;
            this.port = port;
        }
    }

    private static class StubRegistry implements Registry {
        final Map<String, AppStubLocal> apps = new HashMap<>();

        @Override
        public long getRegistryDataVersion() {
            return 0;
        }

        @Override
        public AppStub getAppStub(String instanceId, String appCode) {
            return apps.get(appCode);
        }

        @Override
        public InstanceInfo getLocalInstance() {
            return null;
        }

        @Override
        public List<String> getAllLocalApps() {
            return new ArrayList<>(apps.keySet());
        }

        @Override
        public AppStubLocal getLocalApp(String appCode) {
            return apps.get(appCode);
        }

        @Override
        public List<String> getAllRemoteInstances() {
            return new ArrayList<>();
        }

        @Override
        public InstanceInfo getRemoteInstance(String instanceId) {
            return null;
        }

        @Override
        public List<String> getAllRemoteApps(String instanceId) {
            return null;
        }

        @Override
        public AppStubRemote getRemoteApp(String instanceId, String appCode) {
            return null;
        }
    }

    private static class StubAppStub implements AppStubLocal {
        final AtomicReference<RequestImpl> invoked = new AtomicReference<>();
        private final boolean throwOnInvoke;

        StubAppStub() {
            this(false);
        }

        StubAppStub(boolean throwOnInvoke) {
            this.throwOnInvoke = throwOnInvoke;
        }

        @Override
        public AppMeta getAppMeta() {
            return null;
        }

        @Override
        public AppContext getAppContext() {
            return null;
        }

        @Override
        public void invokeApp(RequestImpl request) throws Throwable {
            invoked.set(request);
            if (throwOnInvoke) {
                throw new IllegalStateException("invoke fail");
            }
            request.getResponse().success(true).data("stub-data");
        }
    }
}
