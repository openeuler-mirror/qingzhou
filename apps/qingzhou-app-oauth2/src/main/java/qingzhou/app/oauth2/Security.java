package qingzhou.app.oauth2;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * 安全工具：回调地址校验、随机串生成、页面内容转义。
 */
final class Security {
    private static final String[] ALLOWED_SCHEMES = {"http", "https"};
    private static final SecureRandom RANDOM = new SecureRandom();

    private Security() {
    }

    /**
     * @return null 表示校验通过，否则为错误描述
     */
    static String validateRedirectUri(String redirectUri) {
        if (isEmpty(redirectUri)) return "redirect_uri 不能为空";

        String lower = redirectUri.toLowerCase(Locale.ROOT);
        boolean schemeAllowed = false; // 阻断 file://、javascript: 等协议，避免授权码泄露到本地文件或脚本
        for (String scheme : ALLOWED_SCHEMES) {
            if (lower.startsWith(scheme + ":/")) {
                schemeAllowed = true;
                break;
            }
        }
        if (!schemeAllowed) return "redirect_uri 仅支持 http/https 协议";

        if (redirectUri.indexOf('\r') >= 0 || redirectUri.indexOf('\n') >= 0
                || lower.contains("%0d") || lower.contains("%0a")) {
            return "redirect_uri 含非法字符"; // 防止 CRLF 注入 Location 响应头
        }
        return null;
    }

    static String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String escapeHtml(String value) {
        if (value == null) return "";

        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(value.charAt(i));
            }
        }
        return escaped.toString();
    }

    static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
