package qingzhou.crypto;

public interface Coder {
    String encode(byte[] data);

    byte[] decode(String data);
}
