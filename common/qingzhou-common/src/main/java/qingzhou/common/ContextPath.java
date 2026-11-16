package qingzhou.common;

/**
 * 全站挂载前缀的归一化契约：启动期配置归一化与注册边界校验共用同一份合法形态。
 * 两边规则漂移会让服务端能启动的配置被中心拒绝，或让危险字符进入中心回连 URL。
 */
public final class ContextPath {
    public static final String DEFAULT = "/qingzhou";
    public static final int MAX_CHARS = 128;

    private ContextPath() {
    }

    // 配置入口：只剥一个尾斜杠；剥多个会把 "//" 变成空串、静默关闭前缀。非法即 fail-fast（激活失败而非全站 404）
    public static String normalize(String configured) {
        String path = configured == null ? null : configured.replaceAll("/$", "");
        // null 由 isNormalized 拒绝；空配置须显式拒绝，否则与配置 "/" 不可区分
        if (!isNormalized(path) || configured.isEmpty()) {
            throw new IllegalArgumentException("qingzhou-http-server.context_path must be a normalized path starting with /, got: " + configured);
        }
        return path;
    }

    // 网络边界：只判定不改写，避免把远端自报值悄悄规范化后放行；空串表示不启用前缀
    public static boolean isNormalized(String path) {
        if (path == null || path.length() > MAX_CHARS) return false;
        if (path.isEmpty()) return true;
        if (!path.startsWith("/")) return false;
        for (String segment : path.substring(1).split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                        || c == '-' || c == '_' || c == '.' || c == '~';
                if (!safe) return false;
            }
        }
        return true;
    }
}
