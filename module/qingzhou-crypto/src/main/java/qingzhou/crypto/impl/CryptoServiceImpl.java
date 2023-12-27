package qingzhou.crypto.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.PasswordCipher;
import qingzhou.crypto.PublicKeyCipher;

import java.nio.charset.StandardCharsets;
import java.security.*;

public class CryptoServiceImpl implements CryptoService {
    private final MessageDigest messageDigest = new MessageDigestImpl();

    @Override
    public PasswordCipher getPasswordCipher(String keySeed) {
        return new PasswordCipherImpl(keySeed);
    }

    @Override
    public PasswordCipher getPasswordCipher(byte[] keySeed) {
        return new PasswordCipherImpl(keySeed);
    }

    @Override
    public String[] generateKeyPair(String seedKey) {
        KeyPair keyPair;
        try {
            keyPair = genKeyPair(seedKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String[] keyPairArray = new String[2];
        keyPairArray[0] = Hex.bytesToHex(publicKey.getEncoded());
        keyPairArray[1] = Hex.bytesToHex(privateKey.getEncoded());
        return keyPairArray;
    }

    @Override
    public PublicKeyCipher getPublicKeyCipher(String publicKey, String privateKey) {
        return new PublicKeyCipherImpl(publicKey, privateKey);
    }

    @Override
    public MessageDigest getMessageDigest() {
        return messageDigest;
    }

    private KeyPair genKeyPair(String seedKey) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(PublicKeyCipherImpl.ALG);//KeyPairGenerator.getInstance(ALGORITHM, pro);
        // windows和linux下SecureRandom的行为不一致
        // 如果使用new SecureRandom(seedKey.getBytes())，在windows会生成相同密钥，在linux会生成不同密钥
        // 因此使用如下方法，确保在相同seedKey下，windows和linux都能生成相同密钥
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(seedKey.getBytes(StandardCharsets.UTF_8));

        kpg.initialize(1024, secureRandom);// 2048 不支持
        return kpg.generateKeyPair();
    }
}
