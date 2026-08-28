package qingzhou.auth.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.TokenService;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;

@Component(configurationPid = "qingzhou-auth")
public class TokenServiceImpl implements TokenService {
    @Reference
    private Crypto crypto;

    private Cipher tokenCipher;
    private long tokenExpireMillis;

    @Activate
    public void init(Map<String, String> config) throws Exception {
        tokenExpireMillis = Integer.parseInt(config.getOrDefault("token_expire_seconds", "" + 30 * 60)) * 1000L;

        String tokenSecret = getSecret();
        tokenCipher = crypto.getCipher(tokenSecret);
    }

    private String getSecret() throws IOException {
        String secret = null;
        Path secretFile = Paths.get(System.getProperty("qingzhou.instance"), "conf", "secret-key.properties");
        if (secretFile.toFile().exists()) {
            try (InputStream inputStream = Files.newInputStream(secretFile, StandardOpenOption.READ)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                secret = properties.getProperty("token");
            }
        }
        if (secret == null || secret.isEmpty()) {
            secret = crypto.generateKey(); // 未配置则随机生成，重启后已签发 token 全部失效
        }
        return secret;
    }

    @Override
    public String createToken(String user) {
        try {
            return tokenCipher.encrypt(user + "|" + (System.currentTimeMillis() + tokenExpireMillis));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            int sep = payload.lastIndexOf('|');
            return System.currentTimeMillis() < Long.parseLong(payload.substring(sep + 1))
                    ? payload.substring(0, sep) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
