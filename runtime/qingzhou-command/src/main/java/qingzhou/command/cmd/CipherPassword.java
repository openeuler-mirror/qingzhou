package qingzhou.command.cmd;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import qingzhou.command.Processor;

public class CipherPassword extends Processor {
    @Override
    public String name() {
        return "cipher-password";
    }

    @Override
    public String info() {
        return "Encrypt plaintext password for configuration in the qingzhou.properties file.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        String password = readPassword(args);
        if (password == null) {
            logSimple("usage: cipher-password <plainPassword>");
            return;
        }

        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Object crypto = loader.loadClass("qingzhou.crypto.impl.CryptoImpl").newInstance();
            Object globalCipher = crypto.getClass().getMethod("getGlobalCipher").invoke(crypto);

            Class<?> cipherClass = loader.loadClass("qingzhou.crypto.Cipher");
            Method encrypt = cipherClass.getMethod("encrypt", String.class);
            String invoke = (String) encrypt.invoke(globalCipher, password);

            logSimple("------------------------- Password  --------------------------");
            logSimple(invoke);
            logSimple("------------------------- End -------------------------------------");
        }
    }
}
