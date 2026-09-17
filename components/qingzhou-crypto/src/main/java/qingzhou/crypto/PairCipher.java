package qingzhou.crypto;

/**
 * null 透传契约：四个重载的 null 输入一律返回 null（保真处理，区分「未提供数据」与「空数据」）。
 */
public interface PairCipher {
    byte[] encryptWithPublicKey(byte[] input) throws Exception;

    String encryptWithPublicKey(String input) throws Exception;

    byte[] decryptWithPrivateKey(byte[] input) throws Exception;

    String decryptWithPrivateKey(String input) throws Exception;
}
