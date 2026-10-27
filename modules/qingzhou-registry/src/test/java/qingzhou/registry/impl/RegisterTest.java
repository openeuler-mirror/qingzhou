package qingzhou.registry.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainService;
import qingzhou.registry.Constants;

/**
 * 注册/续期链路：报文按 agent 侧的真实方式构造（首注册用域密钥、续期用每实例密钥），
 * 覆盖 AAD 绑定（域 + 实例 ID）与实例密钥派生，防止参数错位后链路整体 400 而测试缺位。
 */
public class RegisterTest {
    private static final String DOMAIN = "test-domain";
    private static final String REMOTE_HOST = "127.0.0.1";

    private Path instanceDir;
    private Json json;
    private AgentDomainService agentDomainService;
    private RegistryImpl registry;
    private Register register;

    @BeforeClass
    public void initCrypto() throws Exception {
        instanceDir = Files.createTempDirectory("qingzhou-register-test");
        Path conf = Files.createDirectories(instanceDir.resolve("conf"));
        // 全零密钥仅测试用：长度满足激活期的 256 位校验
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        Files.write(conf.resolve("secret-key.properties"),
                ("agent-domain~" + DOMAIN + ".key=" + key + "\n").getBytes(StandardCharsets.UTF_8));

        CryptoImpl crypto = new CryptoImpl();
        crypto.init(Collections.singletonMap("qingzhou.instance", instanceDir.toString()));

        AgentDomainServiceImpl domainService = new AgentDomainServiceImpl();
        setField(domainService, "crypto", crypto);
        domainService.init(Collections.singletonMap("qingzhou.instance", instanceDir.toString()));
        agentDomainService = domainService;

        JsonImpl jsonImpl = new JsonImpl();
        jsonImpl.init();
        json = jsonImpl;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        Files.deleteIfExists(instanceDir.resolve("conf").resolve("secret-key.properties"));
        Files.deleteIfExists(instanceDir.resolve("conf"));
        Files.deleteIfExists(instanceDir);
    }

    @BeforeMethod
    public void newRegistry() throws Exception {
        registry = new RegistryImpl();
        register = new Register();
        setField(register, "logger", newLoggerStub());
        setField(register, "registry", registry);
        setField(register, "json", json);
        setField(register, "agentDomainService", agentDomainService);
    }

    @Test
    public void firstRegistration_registersInstanceAndReturnsNewId() throws Exception {
        InstanceInfo agentSide = newInstanceInfo();

        CapturedResponse response = handle(null, firstRegistrationBody(agentSide));

        Assert.assertEquals(response.status, 200);
        InstanceInfo registered = registry.getRemoteInstance(registeredInstanceId(response.body));
        Assert.assertNotNull(registered);
        Assert.assertEquals(registered.getInstanceSecret(), agentSide.getInstanceSecret());
        Assert.assertEquals(registered.getDomain(), DOMAIN);
        Assert.assertEquals(registered.getHost(), REMOTE_HOST); // 未开启 trust_advertised_host：不采信自报地址
        Assert.assertTrue(registered.getRefreshTime() > 0);
    }

    @Test
    public void renewal_withInstanceKey_refreshesWithoutNewRegistration() throws Exception {
        InstanceInfo registered = registerNewInstance();
        long before = registered.getRefreshTime();
        Thread.sleep(50); // 大于 Windows 时钟粒度，避免两次刷新落在同一刻度上

        CapturedResponse response = handle(registered.getId(),
                renewalBody(registered.getId(), registered.getInstanceSecret()));

        Assert.assertEquals(response.status, 304);
        Assert.assertTrue(registered.getRefreshTime() > before);
        Assert.assertEquals(registry.getAllRemoteInstances().size(), 1);
    }

    @Test
    public void renewal_withTamperedInstanceId_isRejected() throws Exception {
        InstanceInfo registered = registerNewInstance();

        // AAD 绑定 instanceId：报文对原实例有效，改写 URL 中的实例 ID 后即认证失败
        CapturedResponse response = handle("another-instance",
                renewalBody(registered.getId(), registered.getInstanceSecret()));

        Assert.assertEquals(response.status, 400);
        Assert.assertNull(registry.getRemoteInstance("another-instance"));
    }

    @Test
    public void renewal_withIllegalInstanceId_isRejectedAtBoundary() throws Exception {
        String illegalId = "../" + new String(new char[200]).replace('\0', 'a');
        byte[] body = agentDomainService.getDomainCipher(DOMAIN).encrypt(
                json.toJson(newInstanceInfo()).getBytes(StandardCharsets.UTF_8),
                agentDomainService.aadForRegistry(DOMAIN, illegalId));

        CapturedResponse response = handle(illegalId, body);

        // 该值同时用作注册表键与 AAD 上下文：边界层须在解密与落表前拒绝
        Assert.assertEquals(response.status, 400);
        Assert.assertTrue(registry.getAllRemoteInstances().isEmpty());
    }

    @Test
    public void firstRegistration_withIllegalAppMeta_isRejectedBeforeRegistration() throws Exception {
        InstanceInfo agentSide = newInstanceInfo();
        agentSide.getAppMetas().add(new AppMeta()); // app 为 null 的应用元数据

        CapturedResponse response = handle(null, firstRegistrationBody(agentSide));

        // 畸形元数据不得落表：否则实例一旦注册，远端路由将持续 NPE
        Assert.assertEquals(response.status, 400);
        Assert.assertTrue(registry.getAllRemoteInstances().isEmpty());
    }

    private InstanceInfo registerNewInstance() throws Exception {
        CapturedResponse response = handle(null, firstRegistrationBody(newInstanceInfo()));
        Assert.assertEquals(response.status, 200);
        return registry.getRemoteInstance(registeredInstanceId(response.body));
    }

    private CapturedResponse handle(String instanceId, byte[] body) throws Exception {
        CapturedResponse response = new CapturedResponse();
        register.handle(request(instanceId, body), response);
        return response;
    }

    // agent 首注册：域密钥加密，AAD 无实例 ID
    private byte[] firstRegistrationBody(InstanceInfo instanceInfo) throws Exception {
        return agentDomainService.getDomainCipher(DOMAIN).encrypt(
                json.toJson(instanceInfo).getBytes(StandardCharsets.UTF_8),
                agentDomainService.aadForRegistry(DOMAIN, null));
    }

    // agent 续期：每实例密钥加密，AAD 绑定实例 ID
    private byte[] renewalBody(String instanceId, String instanceSecret) throws Exception {
        EpochKeyRotatingCipher cipher = agentDomainService.getInstanceCipher(DOMAIN, instanceSecret);
        return cipher.encrypt(("{\"id\":\"" + instanceId + "\"}").getBytes(StandardCharsets.UTF_8),
                agentDomainService.aadForRegistry(DOMAIN, instanceId));
    }

    private String registeredInstanceId(byte[] responseBody) throws Exception {
        byte[] instanceId = agentDomainService.getDomainCipher(DOMAIN).decrypt(responseBody,
                agentDomainService.responseAad(agentDomainService.aadForRegistry(DOMAIN, null)));
        return new String(instanceId, StandardCharsets.UTF_8);
    }

    private static HttpRequest request(String instanceId, byte[] body) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.REMOTE_PARAMETER_DOMAIN_ID, DOMAIN);
        if (instanceId != null) parameters.put(Constants.REMOTE_PARAMETER_INSTANCE_ID, instanceId);

        return (HttpRequest) Proxy.newProxyInstance(RegisterTest.class.getClassLoader(),
                new Class<?>[]{HttpRequest.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getParameter".equals(name)) return parameters.get((String) args[0]);
                    if ("getBody".equals(name)) return body;
                    if ("getRemoteHost".equals(name)) return REMOTE_HOST;
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
    }

    private static InstanceInfo newInstanceInfo() {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setInstanceSecret(UUID.randomUUID().toString());
        instanceInfo.setHost("agent.local");
        instanceInfo.setPort(7900);
        instanceInfo.setPlatformVersion("test");
        return instanceInfo;
    }

    private static Logger newLoggerStub() {
        return (Logger) Proxy.newProxyInstance(RegisterTest.class.getClassLoader(),
                new Class<?>[]{Logger.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? Boolean.FALSE : null);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class CapturedResponse implements HttpResponse {
        private int status;
        private byte[] body;

        @Override
        public HttpResponse status(int status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponse header(String name, String value) {
            return this;
        }

        @Override
        public HttpResponse contentType(String value) {
            return this;
        }

        @Override
        public HttpResponse contentTypeJsonUtf8() {
            return this;
        }

        @Override
        public HttpResponse send(String bodyAsUtf8) {
            return send(bodyAsUtf8.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpResponse send(byte[] body) {
            this.body = body;
            if (status == 0) status = 200; // 与框架一致：未显式设置状态码时按 200 成功响应
            return this;
        }

        @Override
        public HttpResponse async() {
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public void redirect(String url) {
        }
    }
}
