package qingzhou.command.cmd;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import qingzhou.command.Processor;

public class PairKey extends Processor {
    @Override
    public String name() {
        return "pair-key";
    }

    @Override
    public String info() {
        return "Initialize a pair of public and private keys for configuration in the qingzhou.properties file.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Class<?> aClass = loader.loadClass("qingzhou.crypto.impl.CryptoImpl");
            Object instance = aClass.newInstance();
            Method generatePairKey = aClass.getMethod("generatePairKey");
            String[] pairKey = (String[]) generatePairKey.invoke(instance);
            logSimple("--------------------------- Public Key ----------------------------");
            logSimple(pairKey[0]);
            logSimple("--------------------------- Private Key ---------------------------");
            logSimple(pairKey[1]);
            logSimple("--------------------------- End -----------------------------------");
        }
    }
}
