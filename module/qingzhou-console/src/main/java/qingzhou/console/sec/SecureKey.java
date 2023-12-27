package qingzhou.console.sec;

import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.pattern.Callback;
import qingzhou.console.util.ExceptionUtil;
import qingzhou.console.util.FileLockUtil;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class SecureKey {
    public static final String remoteKeyName = "remoteKey";
    public static final String localKeyName = "localKey";
    public static final String publicKeyName = "publicKey";
    public static final String privateKeyName = "privateKey";
    public static final String remotePublicKeyName = "remotePublicKey";
    private static final Callback<Void, String> initKey = args -> UUID.randomUUID().toString().replace("-", "");

    private static void checkKey(String checkName) {
        if (remoteKeyName.equals(checkName)
                || localKeyName.equals(checkName)
                || publicKeyName.equals(checkName)
                || privateKeyName.equals(checkName)
                || remotePublicKeyName.equals(checkName)
        ) {
            return;
        }

        throw new IllegalArgumentException("Unsupported name");
    }

    public static String getSecureKey(File baseDir, String key) throws Exception {
        return getSecureKey(baseDir, key, null);
    }

    public static String getSecureKey(File baseDir, String key, Callback<Void, String> init) throws Exception {
        checkKey(key);// 安全检查

        PasswordCipher cipher = Encryptor.maskCipher();// 用默认的密钥加密，方便手工注册添加集中管理的公钥
        File secureFile = getSecureFile(baseDir);
        Properties secure = FileUtil.fileToProperties(secureFile);
        String secVal = secure.getProperty(key);
        if (StringUtil.isBlank(secVal)) {
            if (init != null) {
                secVal = init.run(null);
            }
            if (StringUtil.notBlank(secVal)) {
                secVal = cipher.encrypt(secVal);
                secure.put(key, secVal);
                writeSecureKey(secureFile, StringUtil.propertiesToString(secure));
            }
        }

        if (StringUtil.isBlank(secVal)) {
            return secVal;
        } else {
            secVal = cipher.decrypt(secVal);
            return secVal;
        }
    }

    private static void writeSecureKey(File secureFile, String content) throws IOException {
        Lock lock = FileLockUtil.getFileLock(secureFile).writeLock();
        lock.lock();
        try {
            FileUtil.writeFile(secureFile, content);
        } finally {
            lock.unlock();
        }
    }

    public static void writeSecureKey(File baseDir, String key, String secVal) throws Exception {
        checkKey(key);
        if (StringUtil.isBlank(secVal)) return;

        File secureFile = getSecureFile(baseDir);
        Properties secure = FileUtil.fileToProperties(secureFile);
        PasswordCipher cipher = Encryptor.maskCipher();// 用默认的密钥加密，方便手工注册添加集中管理的公钥
        secVal = cipher.encrypt(secVal);
        secure.put(key, secVal);
        writeSecureKey(secureFile, StringUtil.propertiesToString(secure));
    }

    public static File getSecureFile(File domain) throws IOException {
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

    public static String getOrInitKey(File baseDir, String keyName) throws Exception {
        return SecureKey.getSecureKey(baseDir, keyName, initKey);
    }

    public static synchronized String getOrInitKeyPair(File baseDir, String keyName, String publicKeyName, String privateKeyName) throws Exception {
        final String[] pubKey = new String[1];
        final String[] priKey = new String[1];
        String secureKey = SecureKey.getSecureKey(baseDir, keyName, args -> {
            String seedKey = UUID.randomUUID().toString().replaceAll("-", "");

            if (true) {
                throw new IllegalArgumentException("todo");
            }
//            pubKey[0] = HexUtil.bytesToHex(publicKey.getEncoded());
//            priKey[0] = HexUtil.bytesToHex(privateKey.getEncoded());

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
                SecureKey.getSecureKey(baseDir, privateKeyName, args -> priKey[0]);
            } else if (privateKeyName.equals(keyName)) {
                SecureKey.getSecureKey(baseDir, publicKeyName, args -> pubKey[0]);
            }
        }

        return secureKey;
    }
}
