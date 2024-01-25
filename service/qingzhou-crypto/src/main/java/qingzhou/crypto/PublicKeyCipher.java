package qingzhou.crypto;

public interface PublicKeyCipher {
    String encryptWithPublicKey(String input);

    String decryptWithPrivateKey(String input);

    String encryptWithPrivateKey(String input);

    String decryptWithPublicKey(String input);
}
