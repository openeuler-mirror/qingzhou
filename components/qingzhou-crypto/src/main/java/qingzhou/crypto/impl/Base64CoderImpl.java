package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import qingzhou.crypto.Base64Coder;

class Base64CoderImpl implements Base64Coder {
    @Override
    public String encode(byte[] data) {
        if (data == null) return null;
        return new String(Base64.getEncoder().encode(data), StandardCharsets.ISO_8859_1);
    }

    @Override
    public byte[] decode(String data) {
        if (data == null) return null;
        return java.util.Base64.getDecoder().decode(data.getBytes(StandardCharsets.ISO_8859_1));
    }
}
