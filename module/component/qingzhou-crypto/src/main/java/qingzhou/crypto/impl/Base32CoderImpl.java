package qingzhou.crypto.impl;

import qingzhou.crypto.Base32Coder;

import java.util.Arrays;

class Base32CoderImpl implements Base32Coder {
    private final char[] ALPHABET = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', '2', '3', '4', '5', '6', '7'
    };

    private final byte[] DECODE_TABLE;

    {
        DECODE_TABLE = new byte[128];
        Arrays.fill(DECODE_TABLE, (byte) 0xFF);
        for (int i = 0; i < ALPHABET.length; i++) {
            DECODE_TABLE[ALPHABET[i]] = (byte) i;
            if (i < 24) {
                DECODE_TABLE[Character.toLowerCase(ALPHABET[i])] = (byte) i;
            }
        }
    }

    @Override
    public String encode(byte[] data) {
        char[] chars = new char[((data.length * 8) / 5) + ((data.length % 5) != 0 ? 1 : 0)];

        for (int i = 0, j = 0, index = 0; i < chars.length; i++) {
            if (index > 3) {
                int b = data[j] & (0xFF >> index);
                index = (index + 5) % 8;
                b <<= index;
                if (j < data.length - 1) {
                    b |= (data[j + 1] & 0xFF) >> (8 - index);
                }
                chars[i] = ALPHABET[b];
                j++;
            } else {
                chars[i] = ALPHABET[((data[j] >> (8 - (index + 5))) & 0x1F)];
                index = (index + 5) % 8;
                if (index == 0) {
                    j++;
                }
            }
        }

        return new String(chars);
    }

    @Override
    public byte[] decode(String s) {
        char[] stringData = s.toCharArray();
        byte[] data = new byte[(stringData.length * 5) / 8];

        for (int i = 0, j = 0, index = 0; i < stringData.length; i++) {
            int val = DECODE_TABLE[stringData[i]];

            if (index <= 3) {
                index = (index + 5) % 8;
                if (index == 0) {
                    data[j++] |= (byte) val;
                } else {
                    data[j] |= (byte) (val << (8 - index));
                }
            } else {
                index = (index + 5) % 8;
                data[j++] |= (byte) (val >> index);
                if (j < data.length) {
                    data[j] |= (byte) (val << (8 - index));
                }
            }
        }

        return data;
    }
}
