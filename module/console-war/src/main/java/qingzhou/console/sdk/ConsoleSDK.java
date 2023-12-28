package qingzhou.console.sdk;

import qingzhou.console.util.Base32Util;

import java.nio.charset.StandardCharsets;

public class ConsoleSDK {

    private static final String encodedFlag = "Encoded:";
    private static final String[] encodeFlags = {
            "#", "?", "&",// 一些不能在url中传递的参数
            ":", "%", "+", " ", "=", ",",
            "[", "]"
    };

    /**
     * jsp加密：获取非对称算法公钥 结束
     */
    private ConsoleSDK() {
    }

    public static boolean isEncodedId(String id) {
        return id != null && id.startsWith(encodedFlag);
    }

    public static boolean needEncode(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        for (String flag : encodeFlags) {
            if (id.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，编码后放在url里作参数，因此需要解码
    public static String decodeId(String encodeId) {
        try {
            if (isEncodedId(encodeId)) {
                encodeId = encodeId.substring(encodedFlag.length());
                return new String(Base32Util.decode(encodeId), StandardCharsets.UTF_8); // for #NC-558 特殊字符可能编码了
            }
        } catch (Exception ignored) {
        }
        return encodeId; // 出错，表示 rest 接口，没有编码
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，不能在url里作参数，因此需要编码
    public static String encodeId(String id) {
        try {
            return encodedFlag + Base32Util.encode(id.getBytes(StandardCharsets.UTF_8)); // for #NC-558 特殊字符可能编码了
        } catch (Exception ignored) {
        }
        return id; // 出错，表示 rest 接口，没有编码
    }
}
