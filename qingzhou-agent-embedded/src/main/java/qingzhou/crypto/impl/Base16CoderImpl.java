package qingzhou.crypto.impl;

import qingzhou.crypto.Base16Coder;

public class Base16CoderImpl implements Base16Coder {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    @Override
    public String encode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_CHARS[(b >> 4) & 0xF]);
            sb.append(HEX_CHARS[b & 0xF]);
        }
        return sb.toString();
    }

    @Override
    public byte[] decode(String data) {
        int len = data.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4)
                    + Character.digit(data.charAt(i + 1), 16));
        }
        return result;
    }
}