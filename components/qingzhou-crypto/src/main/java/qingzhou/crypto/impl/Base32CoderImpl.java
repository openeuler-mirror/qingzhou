package qingzhou.crypto.impl;

import qingzhou.crypto.Base32Coder;

class Base32CoderImpl implements Base32Coder {
    // 编码表
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    public String encode(byte[] data) {
        if (data == null) return "";

        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bits = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;

            while (bits >= 5) {
                int index = (buffer >> (bits - 5)) & 31;
                result.append(CHARS.charAt(index));
                bits -= 5;
            }
        }

        // 处理剩余位
        if (bits > 0) {
            buffer <<= (5 - bits);
            result.append(CHARS.charAt(buffer & 31));
        }

        // 添加填充
        while (result.length() % 8 != 0) {
            result.append('=');
        }

        return result.toString();
    }

    @Override
    public byte[] decode(String data) {
        data = data.replace("=", "").toUpperCase();
        if (data.isEmpty()) return new byte[0];

        int byteLen = data.length() * 5 / 8;
        byte[] result = new byte[byteLen];

        int buffer = 0;
        int bits = 0;
        int index = 0;

        for (char c : data.toCharArray()) {
            int value = CHARS.indexOf(c);
            if (value < 0) continue;

            buffer = (buffer << 5) | value;
            bits += 5;

            if (bits >= 8) {
                result[index++] = (byte) (buffer >> (bits - 8));
                bits -= 8;
            }
        }

        return result;
    }
}