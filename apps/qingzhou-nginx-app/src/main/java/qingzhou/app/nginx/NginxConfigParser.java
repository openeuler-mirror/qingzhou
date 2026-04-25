package qingzhou.app.nginx;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/**
 * Nginx配置文件解析和修改工具类
 */
public class NginxConfigParser {

    /**
     * 获取nginx.conf路径 优先使用AppNginx.nginxConfPath（临时目录），否则使用默认路径
     */
    private static String getNginxConfPath() {
        return AppConfig.getNginxConfPath();
    }

    /**
     * 配置项定义：配置名 -> {描述, 区块}
     */
    private static final Map<String, String[]> CONFIG_DEFINITIONS = new LinkedHashMap<>();

    static {
        // 全局配置
        CONFIG_DEFINITIONS.put("user", new String[]{"Nginx进程运行用户", "main"});
        CONFIG_DEFINITIONS.put("worker_processes", new String[]{"Nginx工作进程数（auto=CPU核数）", "main"});
        CONFIG_DEFINITIONS.put("error_log", new String[]{"错误日志路径和级别", "main"});
        CONFIG_DEFINITIONS.put("pid", new String[]{"进程PID保存路径", "main"});

        // events区块配置
        CONFIG_DEFINITIONS.put("worker_connections", new String[]{"每个工作进程最大并发连接数", "events"});

        // stream区块配置
        CONFIG_DEFINITIONS.put("stream.listen", new String[]{"Stream监听端口", "stream"});

        // http区块配置
        CONFIG_DEFINITIONS.put("access_log", new String[]{"访问日志路径和格式", "http"});
        CONFIG_DEFINITIONS.put("gzip", new String[]{"是否启用压缩（on/off）", "http"});
        CONFIG_DEFINITIONS.put("gzip_min_length", new String[]{"最小压缩文件大小", "http"});
        CONFIG_DEFINITIONS.put("gzip_types", new String[]{"压缩的MIME类型", "http"});
        CONFIG_DEFINITIONS.put("keepalive_timeout", new String[]{"保持连接超时时间", "http"});
        CONFIG_DEFINITIONS.put("client_max_body_size", new String[]{"客户端最大请求体大小", "http"});
        CONFIG_DEFINITIONS.put("log_format", new String[]{"日志格式定义", "http"});
        CONFIG_DEFINITIONS.put("default_type", new String[]{"默认MIME类型", "http"});
        CONFIG_DEFINITIONS.put("proxy_connect_timeout", new String[]{"代理连接超时时间", "http"});
        CONFIG_DEFINITIONS.put("proxy_send_timeout", new String[]{"代理发送超时时间", "http"});
        CONFIG_DEFINITIONS.put("proxy_read_timeout", new String[]{"代理接收超时时间", "http"});
        CONFIG_DEFINITIONS.put("proxy_buffer_size", new String[]{"代理缓冲区大小", "http"});
        CONFIG_DEFINITIONS.put("proxy_buffers", new String[]{"代理缓冲区个数和大小", "http"});
        CONFIG_DEFINITIONS.put("proxy_busy_buffers_size", new String[]{"忙碌缓冲区大小", "http"});
        CONFIG_DEFINITIONS.put("proxy_temp_file_write_size", new String[]{"代理临时文件写入大小", "http"});
    }

    /**
     * 解析nginx配置文件，提取常用配置项
     *
     * @return
     * @throws java.io.IOException
     */
    public static Map<String, String> parseConfig() throws IOException {
        Map<String, String> configMap = new LinkedHashMap<>();
        String confPath = getNginxConfPath();
        String content = new String(Files.readAllBytes(Paths.get(confPath)));

        // 遍历所有定义的配置项，统一解析
        for (String directive : CONFIG_DEFINITIONS.keySet()) {
            String[] info = CONFIG_DEFINITIONS.get(directive);
            String block = info[1];
            String value;

            if ("main".equals(block)) {
                // 全局配置直接匹配
                value = extractSimpleDirective(content, directive);
            } else {
                // 块内配置需要指定块名
                value = extractBlockDirective(content, block, directive);
            }

            if (value != null) {
                configMap.put(directive, value);
            }
        }

        return configMap;
    }

    /**
     * 提取简单指令（全局级别）
     */
    private static String extractSimpleDirective(String content, String directive) {
        String regex = directive + "\\s+([^;]+);";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 提取块内指令
     */
    private static String extractBlockDirective(String content, String blockName, String directive) {
        // 找到对应的块
        String blockRegex = blockName + "\\s*\\{([^}]+)\\}";
        Pattern blockPattern = Pattern.compile(blockRegex, Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(content);

        if (blockMatcher.find()) {
            String blockContent = blockMatcher.group(1);
            // 在块内查找指令
            String directiveRegex = directive + "\\s+([^;]+);";
            Pattern directivePattern = Pattern.compile(directiveRegex);
            Matcher directiveMatcher = directivePattern.matcher(blockContent);
            if (directiveMatcher.find()) {
                return directiveMatcher.group(1).trim();
            }
        }
        return null;
    }

    /**
     * 更新配置项
     *
     * @param directive
     * @param newValue
     * @throws java.io.IOException
     */
    public static void updateConfig(String directive, String newValue) throws IOException {
        // 先备份原文件
        backupConfig();

        String confPath = getNginxConfPath();
        Path confPathObj = Paths.get(confPath);
        String content = new String(Files.readAllBytes(confPathObj));
        String[] info = CONFIG_DEFINITIONS.get(directive);

        if (info == null) {
            throw new IllegalArgumentException("不支持的配置项: " + directive);
        }

        String block = info[1];
        String newContent;

        if ("main".equals(block)) {
            // 全局配置直接替换
            newContent = replaceDirective(content, directive, newValue, null);
        } else {
            // 块内配置需要指定块名替换
            newContent = replaceDirective(content, directive, newValue, block);
        }

        if (newContent == null || newContent.equals(content)) {
            throw new IllegalArgumentException("配置项 " + directive + " 不存在或修改失败");
        }

        Files.write(confPathObj, newContent.getBytes());
    }

    /**
     * 替换配置项
     */
    private static String replaceDirective(String content, String directive, String newValue, String block) {
        String regex;
        if (block == null) {
            // 全局配置：匹配 directive value;
            regex = "(" + Pattern.quote(directive) + "\\s+)[^;]+;";
        } else {
            // 块内配置：匹配 block { ... directive value; ... }
            // 使用非贪婪匹配，找到第一个包含该指令的块
            regex = "(" + Pattern.quote(block) + "\\s*\\{[^}]*?)"
                    + "(" + Pattern.quote(directive) + "\\s+)"
                    + "[^;]+"
                    + "(;)";
        }

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            if (block == null) {
                return matcher.replaceFirst("$1" + newValue + ";");
            } else {
                return matcher.replaceFirst("$1$2" + newValue + "$3");
            }
        }

        return null;
    }

    /**
     * 备份配置文件
     *
     * @throws java.io.IOException
     */
    public static void backupConfig() throws IOException {
        File backupDir = new File(AppConfig.getNginxBackupPath());
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = "nginx.conf." + timestamp;
        Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), backupFileName);
        Files.copy(Paths.get(AppConfig.getNginxConfPath()), backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 获取备份文件列表
     *
     * @return
     * @throws java.io.IOException
     */
    public static List<String> getBackupList() throws IOException {
        File backupDir = new File(AppConfig.getNginxBackupPath());
        if (!backupDir.exists()) {
            return Collections.emptyList();
        }

        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("nginx.conf."));
        if (files == null) {
            return Collections.emptyList();
        }

        List<String> backupList = new ArrayList<>();
        Arrays.sort(files, Comparator.comparing(File::lastModified).reversed());
        for (File file : files) {
            backupList.add(file.getName());
        }
        return backupList;
    }

    /**
     * 获取配置项描述
     *
     * @return
     */
    public static Map<String, String> getConfigDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : CONFIG_DEFINITIONS.entrySet()) {
            descriptions.put(entry.getKey(), entry.getValue()[0]);
        }
        return descriptions;
    }

    /**
     * 获取配置项所属区块
     *
     * @param directive
     * @return
     */
    public static String getConfigBlock(String directive) {
        String[] info = CONFIG_DEFINITIONS.get(directive);
        return info != null ? info[1] : "unknown";
    }
}
