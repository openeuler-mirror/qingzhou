package qingzhou.crypto;


public interface MessageDigest {
    String digest(String text, String alg, int saltLength, int iterations);

    boolean matches(String text, String digest);

    String md5(String data);

    byte[] md5(byte[] data);
}
