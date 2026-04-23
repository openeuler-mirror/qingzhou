package qingzhou.command.cmd;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import qingzhou.command.Processor;

public class CipherKey extends Processor {
    @Override
    public String name() {
        return "cipher-key";
    }

    @Override
    public String info() {
        return "Initialize a cipher key for configuration in the qingzhou.properties file.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Class<?> aClass = loader.loadClass("qingzhou.crypto.impl.CryptoImpl");
            Object instance = aClass.newInstance();
            Method generateKey = aClass.getMethod("generateKey");
            String cipherKey = (String) generateKey.invoke(instance);
            logSimple("--------------------------- Cipher Key ----------------------------");
            logSimple(cipherKey);
            logSimple("--------------------------- End -----------------------------------");
        }
    }
}
