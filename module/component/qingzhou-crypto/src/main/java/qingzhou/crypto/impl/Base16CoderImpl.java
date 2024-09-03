package qingzhou.crypto.impl;

import qingzhou.crypto.Base16Coder;

class Base16CoderImpl implements Base16Coder {
    private final char[] DIGITS_UPPER = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

    @Override
    public String encode(byte[] data) {
        if (data == null || data.length == 0) return "";

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
        return decodeHex(data.toCharArray());
    }

    private byte[] decodeHex(char[] data) {
        int len = data.length;
        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j]) << 4;
            j++;
            f = f | toDigit(data[j]);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    private int toDigit(char ch) {
        return Character.digit(ch, 16);
    }
}
