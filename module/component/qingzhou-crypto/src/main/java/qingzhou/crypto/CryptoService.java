package qingzhou.crypto;

public interface CryptoService {
    String generateKey();

    Cipher getCipher(String key);

    String[] generatePairKey();

    PairCipher getPairCipher(String publicKey, String privateKey) throws Exception;

    TotpCipher getTotpCipher();

    MessageDigest getMessageDigest();

    Base64Coder getBase64Coder();

    Base32Coder getBase32Coder();

    Base16Coder getBase16Coder();
}
