package qingzhou.crypto;

public interface HexCoder {
    String bytesToHex(byte[] bytes);

    byte[] hexToBytes(String inHex);
}
