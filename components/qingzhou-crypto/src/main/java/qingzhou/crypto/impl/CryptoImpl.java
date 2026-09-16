package qingzhou.crypto.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.Properties;

import org.osgi.service.component.annotations.Component;
import qingzhou.crypto.*;
import qingzhou.crypto.MessageDigest;

@Component
public class CryptoImpl implements Crypto {
    private final Base64Coder base64Coder = new Base64CoderImpl();
    private final Base32Coder base32Coder = new Base32CoderImpl();
    private final Base16Coder base16Coder = new Base16CoderImpl();

    // MD5 / SHA-1 / SHA-256 等哈希值：当你在下载文件时看到的 MD5 校验码（比如 d41d8cd98f00b204e9800998ecf8427e），本质上就是 Base16（十六进制）编码。
    private final MessageDigest messageDigest = new MessageDigestImpl(base16Coder);
    private final TotpCipher totpCipher = new TotpCipherImpl(base16Coder, base32Coder);

    private volatile Cipher globalCipher;

    @Override
    public String generateKey() {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        return base64Coder.encode(key);
    }

    @Override
    public Cipher getCipher(String key) throws InvalidKeyException {
        if (key == null || key.trim().length() != 24) {
            throw new InvalidKeyException("key length must be 24");
        }
        return new CipherImpl(key.trim(), base64Coder);
    }

    @Override
    public Cipher getGlobalCipher() {
        if (globalCipher == null) {
            synchronized (this) {
                if (globalCipher == null) {
                    try {
                        String secret = getSecret();
                        globalCipher = getCipher(secret);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return globalCipher;
    }

    @Override
    public String[] generatePairKey() {
        KeyPair keyPair;
        try {
            keyPair = genKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String[] keyPairArray = new String[2];
        keyPairArray[0] = base64Coder.encode(publicKey.getEncoded());
        keyPairArray[1] = base64Coder.encode(privateKey.getEncoded());
        return keyPairArray;
    }

    @Override
    public PairCipher getPairCipher(String publicKey, String privateKey) throws InvalidKeyException {
        if ((publicKey == null || publicKey.isEmpty()) && (privateKey == null || privateKey.isEmpty())) {
            throw new InvalidKeyException("public_key or private_key is required, generate a pair with bin/gen-pair-key.sh");
        }
        return new PairCipherImpl(publicKey, privateKey, base64Coder);
    }

    @Override
    public TotpCipher getTotpCipher() {
        return totpCipher;
    }

    @Override
    public MessageDigest getMessageDigest() {
        return messageDigest;
    }

    @Override
    public Base64Coder getBase64Coder() {
        return base64Coder;
    }

    @Override
    public Base32Coder getBase32Coder() {
        return base32Coder;
    }

    @Override
    public Base16Coder getBase16Coder() {
        return base16Coder;
    }

    @Override
    public Base16Coder getHexCoder() {
        return base16Coder;
    }

    private KeyPair genKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(PairCipherImpl.ALG);
        kpg.initialize(2048, new SecureRandom()); // 1024 位已低于安全基线，且 OAEP 分块后载荷过小
        return kpg.generateKeyPair();
    }

    private String getSecret() throws IOException {
        String secret = null;
        Path secretFile = Paths.get(System.getProperty("qingzhou.instance"), "conf", "secret-key.properties");
        if (secretFile.toFile().exists()) {
            try (InputStream inputStream = Files.newInputStream(secretFile, StandardOpenOption.READ)) {
                Properties properties = new Properties();
                properties.load(inputStream);
                secret = properties.getProperty("global");
            }
        }
        if (secret == null || secret.isEmpty()) {
            secret = generateKey(); // 未配置则随机生成，不适用于生产环境
        }
        return secret;
    }
}
