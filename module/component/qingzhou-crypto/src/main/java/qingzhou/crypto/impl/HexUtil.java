package qingzhou.crypto.impl;

class HexUtil {
    /**
     * 字节数组转16进制
     */
    static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        return encodeHexString(bytes);
    }

    /**
     * hex字符串转byte数组
     */
    static byte[] hexToBytes(String inHex) {
        return decodeHex(inHex);
    }

    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    private static String encodeHexString(byte[] data) {
        return new String(encodeHex(data));
    }

    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_UPPER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_UPPER[0x0F & data[i]];
        }
        return out;
    }


    private static byte[] decodeHex(String data) {
        return decodeHex(data.toCharArray());
    }

    private static byte[] decodeHex(char[] data) {

        int len = data.length;

        if ((len & 0x01) != 0) {
            throw new IllegalStateException("Invalid hexadecimal data: Odd number of characters.");
        }

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
            int f = toDigit(data[j], j) << 4;
            j++;
            f = f | toDigit(data[j], j);
            j++;
            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }

    private static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new IllegalStateException("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }
}
