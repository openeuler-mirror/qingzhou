package qingzhou.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 主机地址判定：明文传输准入校验共用的回环识别。
 * 各模块的放行策略与报错文案留在模块内，此处只回答“是不是本机”。
 */
public final class Hosts {
    // 127.0.0.0/8 整段均为回环，各段须校验数值上界
    private static final Pattern IPV4_LOOPBACK = Pattern.compile(
            "127\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){2}");

    // URL.getHost 对 IPv6 字面量返回 "[::1]" 带方括号形式
    public static boolean isLoopback(String host) {
        if (host == null) return false;
        String ip = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(ip) || "::1".equals(ip) || "[::1]".equals(ip) || IPV4_LOOPBACK.matcher(ip).matches();
    }
}
