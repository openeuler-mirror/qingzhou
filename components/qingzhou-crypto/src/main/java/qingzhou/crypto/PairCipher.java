package qingzhou.crypto;

public interface PairCipher {
    byte[] encryptWithPublicKey(byte[] input) throws Exception;

    String encryptWithPublicKey(String input) throws Exception;

    byte[] decryptWithPrivateKey(byte[] input) throws Exception;

    String decryptWithPrivateKey(String input) throws Exception;
}
