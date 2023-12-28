package qingzhou.console.sec;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.ExceptionUtil;
import qingzhou.console.util.HexUtil;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.pattern.Callback;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Encryptor {
    private static final PasswordCipher MASK_CIPHER;
    private static final byte[] defaultBytes = "DONOTCHANGETHISMASK".getBytes(StandardCharsets.UTF_8);

    private static final PasswordCipher LOCAL_CIPHER;
    private static final String publicKeyString;
    private static final Callback<Void, String> initKey = args -> HexUtil.bytesToHex(UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8));

    static {
        // shengcheng  key pair
        String seedKey = UUID.randomUUID().toString().replaceAll("-", "");

        try {
            byte[] tempSec = MessageDigest.getInstance("SHA-256").digest(defaultBytes);
            MASK_CIPHER = ConsoleWarHelper.getPasswordCipher(tempSec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // 编译期间，GenerateDoc 会调用 到这里，导致源码的 *.xml 被篡改（写入了动态密钥），并在源码里生成一个垃圾文件 *.xml.BackUp....
        File domain = ConsoleWarHelper.getDomain();
        try {
            SecureKey.getOrInitKey(domain, SecureKey.remoteKeyName);// 初始化备用
            String localKey = SecureKey.getOrInitKey(domain, SecureKey.localKeyName);
            LOCAL_CIPHER = ConsoleWarHelper.getPasswordCipher(localKey);

            String pubKey = SecureKey.getOrInitKeyPair(domain, SecureKey.publicKeyName, SecureKey.publicKeyName, SecureKey.privateKeyName);
            String priKey = SecureKey.getOrInitKeyPair(domain, SecureKey.privateKeyName, SecureKey.publicKeyName, SecureKey.privateKeyName);
            publicKeyString = pubKey;
        } catch (Exception e) {
            throw ExceptionUtil.unexpectedException(e);
        }
    }

    public static PasswordCipher maskCipher() {
        return MASK_CIPHER;
    }

    public static byte[] encrypt(byte[] pwd) throws Exception {
        return LOCAL_CIPHER.encrypt(pwd);
    }

    public static String encrypt(String pwd) throws Exception {
        return LOCAL_CIPHER.encrypt(pwd);
    }

    public static byte[] decrypt(byte[] password) throws Exception {
        return LOCAL_CIPHER.decrypt(password);
    }

    public static String decrypt(String password) throws Exception {
        return LOCAL_CIPHER.decrypt(password);
    }

    public static String getPublicKeyString() {
        return publicKeyString;
    }

    public static synchronized String getOrInitKey(File baseDir, String keyName) {
        try {
            return SecureKey.getSecureKey(baseDir, keyName, initKey);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Encryptor() {
    }
}
