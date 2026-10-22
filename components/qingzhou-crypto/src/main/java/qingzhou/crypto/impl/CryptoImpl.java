package qingzhou.crypto.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.FileUtil;
import qingzhou.crypto.*;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-crypto", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CryptoImpl implements Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Reference
    private Logger logger;

    private final Base64Coder base64Coder = new Base64CoderImpl();
    private final Base32Coder base32Coder = new Base32CoderImpl();
    private final Base16Coder base16Coder = new Base16CoderImpl();

    // MD5 / SHA-1 / SHA-256 等哈希值：当你在下载文件时看到的 MD5 校验码（比如 d41d8cd98f00b204e9800998ecf8427e），本质上就是 Base16（十六进制）编码。
    private final MessageDigest messageDigest = new MessageDigestImpl(base16Coder);
    private final Totp totp = new TotpImpl(base16Coder, base32Coder);

    private Path secretKeyFile;
    private String globalCipherKeyName;
    private volatile Cipher globalCipher;

    @Activate
    public void init(Map<String, String> config) {
        globalCipherKeyName = config.getOrDefault("global_cipher_key_name", "global");
        secretKeyFile = Paths.get(config.get("qingzhou.instance"), "conf", "secret-key.properties");
        // 全局密钥同样是信任根：不合规先告警再收紧；命令行 init 脱离 OSGi 调用时 logger 未注入
        FileUtil.restrictToOwnerOnly(secretKeyFile.toFile(), msg -> {
            if (logger != null) logger.warn(msg);
        });
    }

    @Override
    public ReplayGuardCipher getReplayGuardCipher(Cipher cipher) {
        return new ReplayGuardCipherImpl(cipher, base64Coder);
    }

    @Override
    public EpochKeyRotatingCipher getEpochKeyRotatingCipher(byte[] masterKey) {
        return new EpochKeyRotatingCipherImpl(this, masterKey);
    }

    @Override
    public String generateCipherKey() {
        byte[] key = new byte[16];
        RANDOM.nextBytes(key);
        return base64Coder.encode(key);
    }

    @Override
    public byte[] generateCipherAad(String... context) {
        if (context == null || context.length == 0) return null;
        return String.join("", context).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Cipher getCipher(String key) throws InvalidKeyException {
        String trimmed = key == null ? null : key.trim();
        if (trimmed == null || trimmed.length() != 24) {
            throw new InvalidKeyException("key length must be 24");
        }
        byte[] cipherKey;
        try {
            cipherKey = base64Coder.decode(trimmed);// 先校验 Base64 合法性：非法字符应在此报 InvalidKeyException，而非构造时抛 IAE 破坏契约
        } catch (IllegalArgumentException e) {
            throw new InvalidKeyException("key is not valid base64", e);
        }
        return getCipher(cipherKey);
    }

    @Override
    public Cipher getCipher(byte[] cipherKey) throws InvalidKeyException {
        // 24 字符 Base64 可解出 16 至 18 字节，而 AES 只接受 16/24/32 字节：在此拦下，避免用错密钥时延迟到加解密才失败
        if (cipherKey.length != 16 && cipherKey.length != 24 && cipherKey.length != 32) {
            throw new InvalidKeyException("key must decode to 16, 24 or 32 bytes: " + cipherKey.length);
        }
        return new CipherImpl(cipherKey, base64Coder, logger);
    }

    @Override
    public Cipher getGlobalCipher() {
        if (globalCipher == null) {
            synchronized (this) {
                if (globalCipher == null) {
                    try {
                        Properties p = new Properties();
                        try (InputStream in = Files.newInputStream(secretKeyFile)) {
                            p.load(in);
                        }
                        String secret = p.getProperty(globalCipherKeyName);
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
    public Totp getTotp() {
        return totp;
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
}
