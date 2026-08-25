package com.tongtech.zookeeper.starter.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZooKeeper JAAS 配置提取工具
 * 兼容 Windows / Linux / macOS，
 * 从 zkEnv.sh、zkEnv.cmd、zkServer.sh、zkServer.cmd 和启动命令中
 * 提取 -Djava.security.auth.login.config 属性值。
 */
public class ZkJaaSConfigExtractor {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final String LOGIN_CONFIG_OPTION = "-Djava.security.auth.login.config=";

    /** 从 .sh 脚本中提取 JAAS 配置的匹配模式 */
    private static final List<Pattern> SH_PATTERNS = Arrays.asList(
            // SERVER_JVMFLAGS="-Djava.security.auth.login.config=/path/to/jaas.conf"
            Pattern.compile("(?:export\\s+)?(?:SERVER_)?JVMFLAGS\\s*=\\s*[\"']([^\"']*?-Djava\\.security\\.auth\\.login\\.config=([^\"'\\s]+))[\"']", Pattern.CASE_INSENSITIVE),
            // ZOOMAIN="-Djava.security.auth.login.config=/path/to/jaas.conf ..."
            Pattern.compile("ZOOMAIN\\s*=\\s*[\"']([^\"']*?-Djava\\.security\\.auth\\.login\\.config=([^\"'\\s]+))[\"']", Pattern.CASE_INSENSITIVE),
            // 直接出现的 -Djava.security.auth.login.config=/path/to/jaas.conf
            Pattern.compile("-Djava\\.security\\.auth\\.login\\.config=([^\\s\"']+)", Pattern.CASE_INSENSITIVE)
    );

    /** 从 .cmd 脚本中提取 JAAS 配置的匹配模式 */
    private static final List<Pattern> CMD_PATTERNS = Arrays.asList(
            // set SERVER_JVMFLAGS=-Djava.security.auth.login.config=C:\path\to\jaas.conf
            Pattern.compile("set\\s+(?:SERVER_)?JVMFLAGS\\s*=\\s*([^\\n]*?-Djava\\.security\\.auth\\.login\\.config=([^\\s\\n]+))", Pattern.CASE_INSENSITIVE),
            // set ZOOMAIN=-Djava.security.auth.login.config=C:\path\to\jaas.conf ...
            Pattern.compile("set\\s+ZOOMAIN\\s*=\\s*([^\\n]*?-Djava\\.security\\.auth\\.login\\.config=([^\\s\\n]+))", Pattern.CASE_INSENSITIVE),
            // 直接出现的 -Djava.security.auth.login.config=C:\path\to\jaas.conf
            Pattern.compile("-Djava\\.security\\.auth\\.login\\.config=([^\\s\\n\"']+)", Pattern.CASE_INSENSITIVE)
    );

    /** 从任意字符串中提取 JAAS 配置路径 */
    private static final Pattern DIRECT_LOGIN_CONFIG_PATTERN = Pattern.compile(
            "-Djava\\.security\\.auth\\.login\\.config=([^\\s\"']+)"
    );

    private ZkJaaSConfigExtractor() {
    }

    /**
     * 从脚本文件中提取 JAAS 配置路径。
     * 脚本不存在或读取失败时返回 null（属于探测性逻辑，不阻塞应用启动）。
     */
    public static String extractFromScript(Path scriptFilePath) {
        try {
            String content = new String(Files.readAllBytes(scriptFilePath));
            String fileName = scriptFilePath.getFileName().toString();
            if (fileName.endsWith(".sh")) {
                return extractFromShScript(content);
            } else if (fileName.endsWith(".cmd")) {
                return extractFromCmdScript(content);
            }
        } catch (IOException e) {
            // 读取失败：脚本可能不存在或无读取权限，忽略即可
        }
        return null;
    }

    /** 从 Shell 脚本 (.sh) 中提取，兼容 Linux / macOS / WSL */
    private static String extractFromShScript(String content) {
        // 移除注释行
        content = content.replaceAll("(?m)^\\s*#.*$", "");
        return extractWithPatterns(content, SH_PATTERNS);
    }

    /** 从 Windows 批处理脚本 (.cmd) 中提取 */
    private static String extractFromCmdScript(String content) {
        // 移除注释行（REM / @ 开头）
        content = content.replaceAll("(?m)^\\s*REM\\s+.*$", "");
        content = content.replaceAll("(?m)^\\s*@.*$", "");
        return extractWithPatterns(content, CMD_PATTERNS);
    }

    private static String extractWithPatterns(String content, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            String value = extractWithPattern(content, pattern);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /** 使用正则表达式从内容中提取 JAAS 配置路径 */
    private static String extractWithPattern(String content, Pattern pattern) {
        Matcher m = pattern.matcher(content);
        if (!m.find()) {
            return null;
        }
        // 优先取第二个捕获组（纯路径），否则取第一个捕获组
        String path = m.groupCount() >= 2 && m.group(2) != null ? m.group(2) : m.group(1);
        if (path == null) {
            return null;
        }
        if (path.startsWith(LOGIN_CONFIG_OPTION)) {
            path = path.substring(LOGIN_CONFIG_OPTION.length());
        }
        path = path.replaceAll("^[\"']|[\"']$", "").trim();
        return IS_WINDOWS ? path.replace('/', '\\') : path.replace('\\', '/');
    }

    /** 从字符串中提取 -Djava.security.auth.login.config=xxx */
    public static String extractJaaSPathFromString(String text) {
        Matcher matcher = DIRECT_LOGIN_CONFIG_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String path = matcher.group(1).replaceAll("^[\"']|[\"']$", "");
        return IS_WINDOWS ? path.replace('/', '\\') : path.replace('\\', '/');
    }
}
