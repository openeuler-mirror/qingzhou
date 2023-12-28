package qingzhou.console.util;

public class Base64Util {
    public static byte[] encode(byte[] src) {
        return java.util.Base64.getEncoder().encode(src);
    }

    public static String encodeToString(byte[] src) {
        return java.util.Base64.getEncoder().encodeToString(src);
    }

    public static byte[] decode(byte[] src) {
        return java.util.Base64.getDecoder().decode(src);
    }

    public static byte[] decode(String src) {
        return java.util.Base64.getDecoder().decode(src);
    }

    private Base64Util() {
    }
}
