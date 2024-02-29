package qingzhou.framework.crypto;

public interface CryptoService {
    KeyCipher getKeyCipher(String keySeed);

    String[] generateKeyPair(String seedKey);

    KeyPairCipher getKeyPairCipher(String publicKey, String privateKey) throws Exception;

    MessageDigest getMessageDigest();
}
