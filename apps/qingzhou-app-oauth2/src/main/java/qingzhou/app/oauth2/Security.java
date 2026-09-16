package qingzhou.app.oauth2;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 安全工具：回调地址与授权范围校验、随机串生成、页面内容转义。
 */
final class Security {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Security() {
    }

    /**
     * 回调地址必须与客户端登记值完全一致，否则授权码或令牌会被投递到攻击者地址。
     *
     * @return null 表示校验通过，否则为错误描述
     */
    static String validateRedirectUri(String requested, String registered) {
        if (isEmpty(registered)) return "客户端未登记 redirect_uri";
        if (!isEmpty(requested) && !requested.equals(registered)) return "redirect_uri 与登记值不一致";

        String lower = registered.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return "redirect_uri 仅支持 http/https 协议";
        if (registered.indexOf('\r') >= 0 || registered.indexOf('\n') >= 0
                || lower.contains("%0d") || lower.contains("%0a")) {
            return "redirect_uri 含非法字符"; // 防止 CRLF 注入 Location 响应头
        }
        return null;
    }

    /**
     * 申请范围不得超出客户端登记范围，否则等于绕过授权范围限制。
     *
     * @return null 表示校验通过，否则为错误描述
     */
    static String validateScope(String scope, String registered) {
        if (isEmpty(scope)) return null;

        Set<String> allowed = split(registered);
        for (String item : split(scope)) {
            if (!allowed.contains(item)) return "scope 超出客户端登记范围：" + item;
        }
        return null;
    }

    static boolean grantAllowed(Map<String, String> client, String grantType) {
        return !isEmpty(grantType) && split(client.get("grant_types")).contains(grantType);
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

    // 客户端授予类型与授权范围均以逗号或空格分隔
    private static Set<String> split(String value) {
        Set<String> items = new LinkedHashSet<>();
        if (value == null) return items;

        for (String item : value.trim().split("[,\\s]+")) {
            if (!item.isEmpty()) items.add(item);
        }
        return items;
    }
}
