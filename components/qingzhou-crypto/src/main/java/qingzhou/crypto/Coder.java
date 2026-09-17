package qingzhou.crypto;

/**
 * Base16/32/64 编码器统一契约：null 透传——encode(null) 返回 null，decode(null) 返回 null，
 * 以区分「未提供数据」与「空数据」；格式非法的输入（如非法字符、奇数长度）则显式抛出 IllegalArgumentException，绝不静默失真。
 */
public interface Coder {
    String encode(byte[] data);

    byte[] decode(String data);
}
