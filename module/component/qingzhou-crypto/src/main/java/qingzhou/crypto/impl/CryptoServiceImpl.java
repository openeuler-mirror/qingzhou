package qingzhou.crypto.impl;

import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.UUID;

public class CryptoServiceImpl implements CryptoService {
    private final Base64Coder base64Coder = new Base64CoderImpl();
    private final Base32Coder base32Coder = new Base32CoderImpl();
    private final Base16Coder base16Coder = new Base16CoderImpl();
    private final MessageDigest messageDigest = new MessageDigestImpl(base64Coder);

    @Override
    public String generateKey() {
        String random = UUID.randomUUID().toString();
        byte[] bytes = CipherImpl.build3desData(random.getBytes());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public Cipher getCipher(String key) {
        return new CipherImpl(key, base64Coder);
    }

    @Override
    public String[] generatePairKey() {
        KeyPair keyPair;
        try {
            String seedKey = UUID.randomUUID().toString().replace("-", "");
            keyPair = genKeyPair(seedKey);
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
    public PairCipher getPairCipher(String publicKey, String privateKey) {
        return new PairCipherImpl(publicKey, privateKey, base64Coder);
    }

    @Override
    public TotpCipher getTotpCipher() {
        return new TotpCipherImpl(base16Coder, base32Coder);
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

    private KeyPair genKeyPair(String seedKey) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(PairCipherImpl.ALG);//KeyPairGenerator.getInstance(ALGORITHM, pro);
        // windows和linux下SecureRandom的行为不一致
        // 如果使用new SecureRandom(seedKey.getBytes())，在windows会生成相同密钥，在linux会生成不同密钥
        // 因此使用如下方法，确保在相同seedKey下，windows和linux都能生成相同密钥
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(seedKey.getBytes(StandardCharsets.UTF_8));

        kpg.initialize(1024, secureRandom);// 2048 不支持
        return kpg.generateKeyPair();
    }
}
