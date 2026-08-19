package com.tongtech.zookeeper.starter.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 ZooKeeper 启动命令中提取安装路径与配置文件路径。
 */
public class ZkPathExtractor {

    /** 匹配 -cp / -classpath 参数（支持引号包裹与等号形式），group(1..3) 为 classpath 内容 */
    private static final Pattern CP_PATTERN = Pattern.compile(
            "(?:-cp|-classpath)\\s*(?:=|\\s+)(?:\"([^\"]+)\"|'([^']+)'|(\\S+))",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 匹配 lib 目录下的 zookeeper jar 包，group(1) 为安装路径前缀。
     * 前缀锚定在 classpath 元素边界（行首 / 冒号 / 空白），避免把 classpath 分隔符
     * （如开头多余的 ':' 或上一元素末尾的 ':'）一并吞入提取结果。
     */
    private static final Pattern JAR_PATTERN = Pattern.compile(
            "(?:^|[:\\\\s])(.*?)[/\\\\]lib[/\\\\]zookeeper[-.][^\\s/\\\\]*\\.jar"
    );

    /** 匹配 zookeeper-<版本>.jar 主 jar 包（优先于 zookeeper-jute 等附属 jar） */
    private static final Pattern MAIN_JAR_PATTERN = Pattern.compile(
            ".*[/\\\\]lib[/\\\\]zookeeper-[0-9.]+\\.[^.]+$"
    );

    /** 匹配 zkServer.sh 默认 classpath 的通配符形式 lib/* 或 lib/*.jar，group(1) 为安装路径前缀（元素边界锚定，同上） */
    private static final Pattern WILDCARD_PATTERN = Pattern.compile(
            "(?:^|[:\\\\s])(.*?)[/\\\\]lib[/\\\\]\\*(?:\\.jar)?"
    );

    /** 匹配 QuorumPeerMain 后的配置文件路径 */
    private static final Pattern CONFIG_PATTERN = Pattern.compile(
            "org\\.apache\\.zookeeper\\.server\\.quorum\\.QuorumPeerMain\\s+" +
                    "(?:\"([^\"]+)\"|'([^']+)'|(\\S+))"
    );

    private ZkPathExtractor() {
    }

    /**
     * 基于 jar 包位置提取安装路径。
     * 支持 zookeeper-3.8.4.jar、zookeeper-jute-3.8.4.jar 等字面 jar，
     * 以及 zkServer.sh 默认的 lib/* 通配符 classpath。
     *
     * @param command ZooKeeper 进程的启动命令行
     * @return 安装路径（已解析 .. 并统一分隔符），无法识别时返回 null
     */
    public static String extractInstallPathFromJar(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        // 优先从 -cp / -classpath 参数的值中提取
        Matcher cpMatcher = CP_PATTERN.matcher(command);
        if (cpMatcher.find()) {
            String classpath = firstNonNullGroup(cpMatcher);
            String installPath = findInstallPath(classpath);
            if (installPath != null) {
                return installPath;
            }
        }
        // 兜底：直接从整条命令中搜索（classpath 缺失或参数形式未识别时）
        return findInstallPath(command);
    }

    /**
     * 在指定文本中查找 zookeeper 安装路径前缀。
     * 优先字面 jar（主 jar zookeeper-<版本>.jar > 其他 lib/zookeeper-*.jar），
     * 其次通配符 lib/*（zkServer.sh 二进制安装包默认形式，需跳过源码编译产物目录）。
     */
    private static String findInstallPath(String text) {
        if (text == null) {
            return null;
        }
        // 1. 字面 jar（最可靠）：lib/zookeeper-<版本>.jar 主 jar 优先，其次其他 lib/zookeeper-*.jar
        //    注意：源码包 zkEnv.sh 的 classpath 可能含 .../zookeeper-server/target/lib/*.jar、
        //    .../build/lib/*.jar 等编译产物通配符，因此必须优先于通配符分支，
        //    否则会误判为 zookeeper-server/target 等非安装目录。
        Matcher jarMatcher = JAR_PATTERN.matcher(text);
        String fallbackPath = null;
        while (jarMatcher.find()) {
            String currentPath = jarMatcher.group(1);
            if (fallbackPath == null) {
                fallbackPath = currentPath;
            }
            if (MAIN_JAR_PATTERN.matcher(jarMatcher.group(0)).matches()) {
                return normalizePath(currentPath);
            }
        }
        if (fallbackPath != null) {
            return normalizePath(fallbackPath);
        }
        // 2. 通配符兜底：zkServer.sh 默认 classpath 为 .../lib/* 或 .../lib/*.jar
        //    跳过源码编译产物目录（.../target/lib/*.jar、.../build/lib/*.jar 等）
        Matcher wildcard = WILDCARD_PATTERN.matcher(text);
        while (wildcard.find()) {
            String prefix = wildcard.group(1);
            if (isBuildArtifactDir(prefix)) {
                continue;
            }
            return normalizePath(prefix);
        }
        return null;
    }

    /** 判断 lib 目录是否位于源码编译产物目录（target / build / src/main/resources）下 */
    private static boolean isBuildArtifactDir(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }
        String lower = prefix.toLowerCase();
        return lower.endsWith("/target") || lower.endsWith("/build")
                || lower.endsWith("/src/main/resources");
    }

    /** 提取配置文件路径 */
    public static String extractConfigPath(String command) {
        Matcher matcher = CONFIG_PATTERN.matcher(command);
        if (matcher.find()) {
            return normalizePath(firstNonNullGroup(matcher));
        }
        return null;
    }

    /** 返回 matcher 中第一个非 null 的捕获组 */
    private static String firstNonNullGroup(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String group = matcher.group(i);
            if (group != null) {
                return group;
            }
        }
        return null;
    }

    /** 规范化路径：解析 .. 并统一分隔符 */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            Path resolved = Paths.get(path.replace('\\', '/')).normalize();
            return resolved.toString().replace('\\', '/');
        } catch (Exception e) {
            return path.replace('\\', '/');
        }
    }
}
