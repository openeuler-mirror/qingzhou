package qingzhou.crypto.impl;

import java.util.Base64;

import qingzhou.crypto.Base32Coder;

public class Base32CoderImpl implements Base32Coder {
    private static final char[] BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] DECODE_TABLE = new int[128];

    static {
        for (int i = 0; i < 128; i++) DECODE_TABLE[i] = -1;
        for (int i = 0; i < BASE32_CHARS.length; i++) {
            DECODE_TABLE[BASE32_CHARS[i]] = i;
            DECODE_TABLE[Character.toLowerCase(BASE32_CHARS[i])] = i;
        }
    }

    @Override
    public String encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS[(buffer >> (bitsLeft - 5)) & 0x1F]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS[(buffer << (5 - bitsLeft)) & 0x1F]);
        }
        return sb.toString();
    }

    @Override
    public byte[] decode(String data) {
        String upper = data.toUpperCase().replaceAll("=", "");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c >= 128 || DECODE_TABLE[c] == -1) continue;
            buffer = (buffer << 5) | DECODE_TABLE[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}