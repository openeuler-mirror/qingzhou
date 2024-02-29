package qingzhou.framework.crypto;

public interface KeyPairCipher {
    String encryptWithPublicKey(String input);

    String decryptWithPrivateKey(String input);

    String encryptWithPrivateKey(String input);

    String decryptWithPublicKey(String input);
}
