package qingzhou.crypto.impl;

import qingzhou.crypto.KeyManager;
import qingzhou.crypto.PasswordCipher;
import qingzhou.framework.pattern.Callback;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;

public class KeyManagerImpl implements KeyManager {
    private static final Callback<Void, String> DEFAULT_INIT_KEY = args -> UUID.randomUUID().toString().replace("-", "");

    private static final PasswordCipher MASK_CIPHER;

    static {
        try {
            byte[] defaultBytes = "DONOTCHANGETHISMASK".getBytes(StandardCharsets.UTF_8);
            byte[] tempSec = MessageDigest.getInstance("SHA-256").digest(defaultBytes);
            MASK_CIPHER = new PasswordCipherImpl(tempSec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getKey(File keyFile, String keyName) throws Exception {
        return getKeyOrElseInit(keyFile, keyName, null);
    }

    @Override
    public String getKeyOrElseInit(File keyFile, String keyName, Callback<Void, String> initKey) throws Exception {
        Properties secure = FileUtil.fileToProperties(keyFile);
        String secVal = secure.getProperty(keyName);
        if (StringUtil.isBlank(secVal)) {
            if (initKey == null) initKey = DEFAULT_INIT_KEY;
            secVal = initKey.run(null);
            if (StringUtil.notBlank(secVal)) {
                secVal = MASK_CIPHER.encrypt(secVal);
                secure.put(keyName, secVal);
                FileUtil.writeFile(keyFile, StringUtil.propertiesToString(secure));
            }
        }

        if (!StringUtil.isBlank(secVal)) {
            secVal = MASK_CIPHER.decrypt(secVal);
        }
        return secVal;
    }

    @Override
    public void writeKey(File keyFile, String keyName, String keyVal) throws Exception {
        if (StringUtil.isBlank(keyVal)) return;

        Properties secure = FileUtil.fileToProperties(keyFile);
        keyVal = MASK_CIPHER.encrypt(keyVal);
        secure.put(keyName, keyVal);
        FileUtil.writeFile(keyFile, StringUtil.propertiesToString(secure));
    }

    @Override
    public String getKeyPair(File keyFile, String keyName, String publicKeyName, String privateKeyName) throws Exception {
        return getKeyPairOrElseInit(keyFile, keyName, publicKeyName, privateKeyName, null);
    }

    @Override
    public String getKeyPairOrElseInit(File keyFile, String keyName, String publicKeyName, String privateKeyName, Callback<Void, String[]> initKey) throws Exception {
        final String[] pubKey = new String[1];
        final String[] priKey = new String[1];
        String secureKey = getKeyOrElseInit(keyFile, keyName, args -> {
            Callback<Void, String[]> initKeyPair = initKey;
            if (initKeyPair == null) {
                initKeyPair = args1 -> new CryptoServiceImpl().generateKeyPair(DEFAULT_INIT_KEY.run(null));
            }
            String[] keyPair = initKeyPair.run(null);
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
                writeKey(keyFile, privateKeyName, priKey[0]);
            } else if (privateKeyName.equals(keyName)) {
                writeKey(keyFile, publicKeyName, pubKey[0]);
            }
        }

        return secureKey;
    }
}
