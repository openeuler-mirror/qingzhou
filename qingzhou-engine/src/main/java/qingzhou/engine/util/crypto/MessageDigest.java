package qingzhou.engine.util.crypto;


public interface MessageDigest {
    String digest(String text, String algorithm, int saltLength, int iterations);

    boolean matches(String text, String msgDigest);

    String fingerprint(String data);

    byte[] hexToBytes(String encode);

    String bytesToHex(byte[] bytes);
}
