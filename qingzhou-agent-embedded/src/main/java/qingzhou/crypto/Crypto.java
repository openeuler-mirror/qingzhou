package qingzhou.crypto;

public interface Crypto {
    String generateKey();
    Cipher getCipher(String key) throws Exception;
    String[] generatePairKey();
    PairCipher getPairCipher(String publicKey, String privateKey) throws Exception;
    TotpCipher getTotpCipher();
    MessageDigest getMessageDigest();
    Base64Coder getBase64Coder();
    Base32Coder getBase32Coder();
    Base16Coder getBase16Coder();
}