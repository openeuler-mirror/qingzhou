package qingzhou.command.cmd;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import qingzhou.command.Processor;

public class TotpKey extends Processor {
    @Override
    public String name() {
        return "totp-key";
    }

    @Override
    public String info() {
        return "Initialize a totp key for configuration in the qingzhou.properties file.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Class<?> aClass = loader.loadClass("qingzhou.crypto.impl.CryptoImpl");
            Object instance = aClass.newInstance();
            Method getTotpCipher = aClass.getMethod("getTotpCipher");
            Object totpCipher = getTotpCipher.invoke(instance);

            Class<?> totpCipherClass = loader.loadClass("qingzhou.crypto.impl.TotpCipherImpl");
            Method generateKey = totpCipherClass.getMethod("generateKey");
            generateKey.setAccessible(true);
            String totpKey = (String) generateKey.invoke(totpCipher);

            logSimple("--------------------------- Totp Key ----------------------------");
            logSimple(totpKey);
            logSimple("--------------------------- End -----------------------------------");
        }
    }
}
