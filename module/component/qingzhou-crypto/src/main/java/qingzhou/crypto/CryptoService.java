package qingzhou.crypto;

public interface CryptoService {
    String generateKey();

    KeyCipher getKeyCipher(String keySeed);

    String[] generateKeyPair(String seedKey);

    KeyPairCipher getKeyPairCipher(String publicKey, String privateKey) throws Exception;

    MessageDigest getMessageDigest();

    HexCoder getHexCoder();
}
