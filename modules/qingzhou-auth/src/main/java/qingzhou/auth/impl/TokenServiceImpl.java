package qingzhou.auth.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.TokenService;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TokenServiceImpl implements TokenService {
    @Reference
    private Crypto crypto;

    private long tokenExpireMillis;
    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        tokenExpireMillis = Integer.parseInt(config.getOrDefault("token_expire_seconds", "" + 30 * 60)) * 1000L;
        tokenCipher = CipherManager.getInstance(crypto).getCipher();
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
