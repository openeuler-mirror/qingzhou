package qingzhou.crypto.impl;

import qingzhou.crypto.Base16Coder;

/**
 * Base16 本质上就是字节转十六进制（Hex）
 */
class Base16CoderImpl implements Base16Coder {
    private static final char[] DIGITS_UPPER = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

    @Override
    public String encode(byte[] data) {
        if (data == null) return null;
        if (data.length == 0) return "";

        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_UPPER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_UPPER[0x0F & data[i]];
        }

        return new String(out);
    }

    @Override
    public byte[] decode(String data) {
        if (data == null) return null;
        return decodeHex(data.toCharArray());
    }

    private byte[] decodeHex(char[] data) {
        int len = data.length;
        if (len % 2 != 0) throw new IllegalArgumentException("odd number of hex characters: " + len);

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j++]) << 4;
            f = f | toDigit(data[j++]);
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    private int toDigit(char ch) {
        int digit = Character.digit(ch, 16);
        if (digit < 0) throw new IllegalArgumentException("illegal hex character: " + ch);
        return digit;
    }
}
