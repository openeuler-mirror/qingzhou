package qingzhou.crypto;

import qingzhou.engine.Service;

@Service(name = "Encryption Tools", description = "Provide tools for encryption and decryption.")
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
