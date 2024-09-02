package qingzhou.crypto;

public interface CryptoService {
    String generateKey();

    Cipher getCipher(String key);

    String[] generatePairKey();

    PairCipher getPairCipher(String publicKey, String privateKey) throws Exception;

    TotpCipher getTotpCipher();

    MessageDigest getMessageDigest();

    HexCoder getHexCoder();
}
