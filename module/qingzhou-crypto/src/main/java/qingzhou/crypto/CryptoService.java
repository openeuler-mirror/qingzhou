package qingzhou.crypto;

public interface CryptoService {
    PasswordCipher getPasswordCipher(String keySeed);

    PasswordCipher getPasswordCipher(byte[] keySeed);

    String[] generateKeyPair(String seedKey);

    PublicKeyCipher getPublicKeyCipher(String publicKey, String privateKey) throws Exception;

    MessageDigest getMessageDigest();
}
