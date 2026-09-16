package qingzhou.auth;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = TokenService.class)
public class TokenService {
    private static final String USER_SP = "@";
    private static final String ROLES_SP = ",";

    @Reference
    private Crypto crypto;

    private long tokenExpireMillis;
    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        tokenExpireMillis = Integer.parseInt(config.getOrDefault("token_expire_seconds", "" + 30 * 60)) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
    }

    public String createToken(String user, String[] roles) {
        try {
            StringBuilder roleStr = new StringBuilder();
            if (roles != null) {
                for (String role : roles) {
                    if (roleStr.length() > 0) {
                        roleStr.append(ROLES_SP);
                    }
                    roleStr.append(role);
                }
            }
            return tokenCipher.encrypt(user + USER_SP + roleStr + USER_SP + (System.currentTimeMillis() + tokenExpireMillis));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Object[] verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            String[] sep = payload.split(USER_SP);
            return System.currentTimeMillis() < Long.parseLong(sep[2])
                    ? new Object[]{sep[0], sep[1].split(ROLES_SP)} : null;
        } catch (Exception e) {
            return null;
        }
    }
}
