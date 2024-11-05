package qingzhou.crypto;

import qingzhou.engine.ServiceInfo;

public interface CryptoService extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to encryption and decryption.";
    }

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
