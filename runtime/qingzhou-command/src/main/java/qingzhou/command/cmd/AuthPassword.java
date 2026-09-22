package qingzhou.command.cmd;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import qingzhou.command.Processor;

public class AuthPassword extends Processor {
    @Override
    public String name() {
        return "auth-password";
    }

    @Override
    public String info() {
        return "Generate the digest of the admin password for configuration in the qingzhou.properties file.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        String password = readPassword(args);
        if (password == null) {
            logSimple("usage: auth-password <plainPassword>");
            return;
        }

        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Object crypto = loader.loadClass("qingzhou.crypto.impl.CryptoImpl").newInstance();
            Object messageDigest = crypto.getClass().getMethod("getMessageDigest").invoke(crypto);

            // 从接口取 Method：实现类是包级私有的，直接用其 Class 反射会 invoke 失败
            Class<?> messageDigestApi = loader.loadClass("qingzhou.crypto.MessageDigest");
            Method digest = messageDigestApi.getMethod("digest", String.class, String.class, int.class, int.class);

            logSimple("------------------------- Password Digest --------------------------");
            logSimple((String) digest.invoke(messageDigest, password, "SHA-256", 16, 2));
            logSimple("------------------------- End -------------------------------------");
        }
    }
}
