package qingzhou.auth.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;

class CipherManager {
    private static CipherManager instance;

    static CipherManager getInstance(Crypto crypto) {
        if (instance == null) {
            instance = new CipherManager(crypto);
        }
        return instance;
    }

    private final Crypto crypto;
    private Cipher useCipher;

    private CipherManager(Crypto crypto) {
        this.crypto = crypto;
    }

    Cipher getCipher() {
        if (useCipher == null) {
            try {
                String secret = getSecret();
                useCipher = crypto.getCipher(secret);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return useCipher;
    }


    private String getSecret() throws IOException {
        String secret = null;
        Path secretFile = Paths.get(System.getProperty("qingzhou.instance"), "conf", "secret-key.properties");
        if (secretFile.toFile().exists()) {
            try (InputStream inputStream = Files.newInputStream(secretFile, StandardOpenOption.READ)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                secret = properties.getProperty("auth");
            }
        }
        if (secret == null || secret.isEmpty()) {
            secret = crypto.generateKey(); // 未配置则随机生成，不适用于生产环境
        }
        return secret;
    }
}
