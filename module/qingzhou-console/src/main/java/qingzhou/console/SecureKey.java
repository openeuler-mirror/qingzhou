package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.impl.ServerUtil;
import qingzhou.framework.pattern.Callback;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;

public class SecureKey {
    public static final String remoteKeyName = "remoteKey";
    public static final String localKeyName = "localKey";
    public static final String publicKeyName = "publicKey";
    public static final String privateKeyName = "privateKey";
    public static final String remotePublicKeyName = "remotePublicKey";
    private static final Callback<Void, String> initKey = args -> UUID.randomUUID().toString().replace("-", "");

    private static final PasswordCipher MASK_CIPHER;
    private static final String publicKeyString;
    private static final String privateKeyString;

    static {
        try {
            byte[] defaultBytes = "DONOTCHANGETHISMASK".getBytes(StandardCharsets.UTF_8);
            byte[] tempSec = MessageDigest.getInstance("SHA-256").digest(defaultBytes);
            MASK_CIPHER = ConsoleWarHelper.getCryptoService().getPasswordCipher(tempSec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // 编译期间，GenerateDoc 会调用 到这里，导致源码的 *.xml 被篡改（写入了动态密钥），并在源码里生成一个垃圾文件 *.xml.BackUp....
        File domain = ServerUtil.getDomain();
        try {
            String pubKey = SecureKey.getOrInitKeyPair(domain, SecureKey.publicKeyName, SecureKey.publicKeyName, SecureKey.privateKeyName);
            String priKey = SecureKey.getOrInitKeyPair(domain, SecureKey.privateKeyName, SecureKey.publicKeyName, SecureKey.privateKeyName);
            publicKeyString = pubKey;
            privateKeyString = priKey;
        } catch (Exception e) {
            throw ExceptionUtil.unexpectedException(e);
        }
    }

    public static String getPrivateKeyString() {
        return privateKeyString;
    }

    public static String getPublicKeyString() {
        return publicKeyString;
    }


    public static synchronized String getSecureKey(File baseDir, String key, Callback<Void, String> init) throws Exception {
        File secureFile = getSecureFile(baseDir);
        Properties secure = FileUtil.fileToProperties(secureFile);
        String secVal = secure.getProperty(key);
        if (StringUtil.isBlank(secVal)) {
            if (init != null) {
                secVal = init.run(null);
            }
            if (StringUtil.notBlank(secVal)) {
                secVal = MASK_CIPHER.encrypt(secVal);
                secure.put(key, secVal);
                FileUtil.writeFile(secureFile, StringUtil.propertiesToString(secure));
            }
        }

        if (StringUtil.isBlank(secVal)) {
            return secVal;
        } else {
            secVal = MASK_CIPHER.decrypt(secVal);
            return secVal;
        }
    }

    public static synchronized void writeSecureKey(File domain, String key, String secVal) throws Exception {
        if (StringUtil.isBlank(secVal)) return;

        File secureFile = getSecureFile(domain);
        Properties secure = FileUtil.fileToProperties(secureFile);
        secVal = MASK_CIPHER.encrypt(secVal);
        secure.put(key, secVal);
        FileUtil.writeFile(secureFile, StringUtil.propertiesToString(secure));
    }

    public static synchronized String getOrInitKey(File domain, String keyName) throws Exception {
        return SecureKey.getSecureKey(domain, keyName, initKey);
    }

    public static synchronized String getOrInitKeyPair(File domain, String keyName, String publicKeyName, String privateKeyName) throws Exception {
        final String[] pubKey = new String[1];
        final String[] priKey = new String[1];
        String secureKey = SecureKey.getSecureKey(domain, keyName, args -> {
            String[] keyPair = ConsoleWarHelper.getCryptoService().generateKeyPair(initKey.run(null));
            pubKey[0] = keyPair[0];
            priKey[0] = keyPair[1];

            if (keyName.equals(publicKeyName)) {
                return pubKey[0];
            } else if (keyName.equals(privateKeyName)) {
                return priKey[0];
            }

            return null;
        });

        // 检测是否是首次生成密钥对，需要成对写入
        if (pubKey[0] != null && priKey[0] != null) {
            if (keyName.equals(publicKeyName)) {
                SecureKey.getSecureKey(domain, privateKeyName, args -> priKey[0]);
            } else if (privateKeyName.equals(keyName)) {
                SecureKey.getSecureKey(domain, publicKeyName, args -> pubKey[0]);
            }
        }

        return secureKey;
    }

    private static synchronized File getSecureFile(File domain) throws IOException {
        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        File secureFile = FileUtil.newFile(secureDir, "secure");
        if (!secureFile.exists()) {
            if (!secureFile.createNewFile()) {
                throw ExceptionUtil.unexpectedException(secureFile.getPath());
            }
        }

        return secureFile;
    }
}
