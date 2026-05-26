package qingzhou.crypto.impl;

import java.util.Base64;

import qingzhou.crypto.Base64Coder;

public class Base64CoderImpl implements Base64Coder {
    @Override
    public String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    @Override
    public byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }
}