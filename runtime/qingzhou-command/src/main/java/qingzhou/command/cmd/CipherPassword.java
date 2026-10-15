package qingzhou.command.cmd;

import java.io.Console;
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
        // 加密须用目标实例的全局密钥：口令占 args[0]，实例名占 args[1]（可选），交互时引导输入，缺省 default
        String instance = args.length > 1 && !args[1].isEmpty() ? args[1] : null;
        if (instance == null) {
            Console console = System.console();
            String input = console == null ? null : console.readLine("instance name [default]: ");
            instance = input == null || input.trim().isEmpty() ? "default" : input.trim();
        }
        if (initInstance(new String[]{instance}) == null) return;

        String password = readPassword(args);
        if (password == null) {
            log("usage: cipher-password <plainPassword> [instance]");
            return;
        }

        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl})) {
            Object crypto = loader.loadClass("qingzhou.crypto.impl.CryptoImpl").getDeclaredConstructor().newInstance();
            Object globalCipher = crypto.getClass().getMethod("getGlobalCipher").invoke(crypto);

            Class<?> cipherClass = loader.loadClass("qingzhou.crypto.Cipher");
            Method encrypt = cipherClass.getMethod("encrypt", String.class);
            String invoke = (String) encrypt.invoke(globalCipher, password);

            log("------------------------- Password  --------------------------");
            log(invoke);
            log("------------------------- End -------------------------------------");
        }
    }
}
