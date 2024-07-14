package qingzhou.crypto.impl;

import qingzhou.crypto.HexCoder;

class HexCoderImpl implements HexCoder {
    @Override
    public String bytesToHex(byte[] bytes) {
        return HexUtil.bytesToHex(bytes);
    }

    @Override
    public byte[] hexToBytes(String inHex) {
        return HexUtil.hexToBytes(inHex);
    }
}
