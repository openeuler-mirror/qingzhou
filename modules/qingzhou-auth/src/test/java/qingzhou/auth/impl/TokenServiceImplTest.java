package qingzhou.auth.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.impl.CryptoImpl;

public class TokenServiceImplTest {

    @Test
    public void randomSecret_createTokenThenVerify_returnsUser() throws Exception {
        File instanceDir = Files.createTempDirectory("qz-instance-").toFile();
        try {
            TokenServiceImpl service = buildService(instanceDir, "60"); // 无 secret 文件 → 随机密钥

            String token = service.createToken("admin");

            Assert.assertNotNull(token);
            Assert.assertEquals(service.verifyToken(token), "admin");
        } finally {
            deleteRecursively(instanceDir);
        }
    }

    @Test
    public void fixedSecret_createTokenThenVerify_returnsUser() throws Exception {
        File instanceDir = Files.createTempDirectory("qz-instance-").toFile();
        try {
            String secret = new CryptoImpl().generateKey(); // 合法 Base64 24 字符
            writeSecretFile(instanceDir, secret);
            TokenServiceImpl service = buildService(instanceDir, "60");

            String token = service.createToken("admin");

            Assert.assertEquals(service.verifyToken(token), "admin");
        } finally {
            deleteRecursively(instanceDir);
        }
    }

    @Test
    public void expiredToken_verify_returnsNull() throws Exception {
        File instanceDir = Files.createTempDirectory("qz-instance-").toFile();
        try {
            TokenServiceImpl service = buildService(instanceDir, "0"); // 过期时间为 0，签发即过期

            String token = service.createToken("admin");

            Assert.assertEquals(service.verifyToken(token), null);
        } finally {
            deleteRecursively(instanceDir);
        }
    }

    @Test
    public void invalidToken_verify_returnsNull() throws Exception {
        File instanceDir = Files.createTempDirectory("qz-instance-").toFile();
        try {
            TokenServiceImpl service = buildService(instanceDir, "60");

            Assert.assertEquals(service.verifyToken("not-a-valid-token"), null);
        } finally {
            deleteRecursively(instanceDir);
        }
    }

    // ---------- 辅助 ----------

    private TokenServiceImpl buildService(File instanceDir, String expireSeconds) throws Exception {
        TokenServiceImpl service = new TokenServiceImpl();
        setField(service, "crypto", new CryptoImpl());

        Map<String, String> config = new HashMap<>();
        config.put("token_expire_seconds", expireSeconds);
        String original = System.getProperty("qingzhou.instance");
        System.setProperty("qingzhou.instance", instanceDir.getAbsolutePath()); // getSecret 依赖该属性
        try {
            service.init(config);
        } finally {
            if (original != null) {
                System.setProperty("qingzhou.instance", original);
            } else {
                System.clearProperty("qingzhou.instance");
            }
        }
        return service;
    }

    private void writeSecretFile(File instanceDir, String secret) throws Exception {
        File confDir = new File(instanceDir, "conf");
        Files.createDirectories(confDir.toPath());
        Files.write(new File(confDir, "secret-key.properties").toPath(),
                ("token=" + secret).getBytes(StandardCharsets.UTF_8));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
}
