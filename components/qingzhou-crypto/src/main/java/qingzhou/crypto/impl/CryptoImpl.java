package qingzhou.crypto.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.*;
import qingzhou.crypto.MessageDigest;
import qingzhou.logger.Logger;

@Component
public class CryptoImpl implements Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Reference
    private Logger logger;

    private final Base64Coder base64Coder = new Base64CoderImpl();
    private final Base32Coder base32Coder = new Base32CoderImpl();
    private final Base16Coder base16Coder = new Base16CoderImpl();

    // MD5 / SHA-1 / SHA-256 等哈希值：当你在下载文件时看到的 MD5 校验码（比如 d41d8cd98f00b204e9800998ecf8427e），本质上就是 Base16（十六进制）编码。
    private final MessageDigest messageDigest = new MessageDigestImpl(base16Coder);
    private final TotpCipher totpCipher = new TotpCipherImpl(base16Coder, base32Coder);

    private volatile Cipher globalCipher;

    @Override
    public ReplayGuardCipher getReplayGuardCipher(Cipher cipher) {
        return new ReplayGuardCipherImpl(cipher);
    }

    @Override
    public String generateKey() {
        byte[] key = new byte[16];
        RANDOM.nextBytes(key);
        return base64Coder.encode(key);
    }

    @Override
    public Cipher getCipher(String key) throws InvalidKeyException {
        if (key == null || key.trim().length() != 24) {
            throw new InvalidKeyException("key length must be 24");
        }
        byte[] cipherKey;
        try {
            cipherKey = base64Coder.decode(key.trim());// 先校验 Base64 合法性：非法字符应在此报 InvalidKeyException，而非构造时抛 IAE 破坏契约
        } catch (IllegalArgumentException e) {
            throw new InvalidKeyException("key is not valid base64", e);
        }
        return new CipherImpl(cipherKey, base64Coder, logger);
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
            throw new InvalidKeyException("public_key or private_key is required, generate a pair with bin/qingzhou.sh pair-key");
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

    private KeyPair genKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(PairCipherImpl.ALG);
        kpg.initialize(2048, RANDOM); // 1024 位已低于安全基线，且 OAEP 分块后载荷过小
        return kpg.generateKeyPair();
    }

    private String getSecret() throws IOException {
        String instanceDir = System.getProperty("qingzhou.instance");
        if (instanceDir == null || instanceDir.trim().isEmpty()) {
            // fail-fast 给出可读错误，而非 Paths.get(null) 的裸 NPE 被包装成误导性的 IllegalStateException
            throw new IllegalStateException("system property 'qingzhou.instance' is required to locate the global secret key");
        }
        Path secretFile = Paths.get(instanceDir, "conf", "secret-key.properties");
        String content = secretFile.toFile().isFile()
                ? new String(Files.readAllBytes(secretFile), StandardCharsets.UTF_8) : "";
        String secret = getValue(content, "global");
        if (isBlank(secret)) {
            secret = generateKey();
            saveSecret(secretFile, putValue(content, "global", secret)); // 首次使用即持久化，此后令牌与密文始终有效
            if (logger != null) { // 命令行工具经反射构造本类时无 DS 注入，logger 为 null
                logger.info("global secret key generated and saved to " + secretFile
                        + "; copy it to peer nodes so that all cluster members share the same key");
            }
        }
        return secret;
    }

    // 先写临时文件再原子替换，避免半写损坏；全局密钥落盘即收敛权限，不留明文窗口
    private void saveSecret(Path secretFile, String content) throws IOException {
        Path temp = Paths.get(secretFile.toString() + ".tmp");
        Files.write(temp, content.getBytes(StandardCharsets.UTF_8));
        chmod600(temp);
        Files.move(temp, secretFile, StandardCopyOption.REPLACE_EXISTING);
        chmod600(secretFile);
    }

    private void chmod600(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (Exception ignored) { // Windows 等无 POSIX 权限模型的平台跳过
        }
    }

    private static String getValue(String content, String key) {
        for (String line : content.split("\n", -1)) {
            String trim = line.trim();
            if (trim.startsWith(key + "=")) return trim.substring(key.length() + 1).trim();
        }
        return null; // 无该配置项返回 null，有但为空返回空串
    }

    private static String putValue(String content, String key, String value) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith(key + "=")) {
                // 有该配置项则整行替换，注释原样保留；保留行尾 \r，避免 CRLF 文件被替换后行尾混杂
                String cr = lines[i].endsWith("\r") ? "\r" : "";
                lines[i] = key + "=" + value + cr;
                return String.join("\n", lines);
            }
        }
        return content + (content.isEmpty() || content.endsWith("\n") ? "" : "\n") + key + "=" + value + "\n";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
