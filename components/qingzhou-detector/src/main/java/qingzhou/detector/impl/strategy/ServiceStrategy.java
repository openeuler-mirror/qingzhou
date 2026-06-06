package qingzhou.detector.impl.strategy;

import qingzhou.detector.ApplicationProfile;
import qingzhou.detector.DetectionStrategy;
import qingzhou.detector.PathResult;
import qingzhou.detector.impl.PathDerivationUtil;
import qingzhou.detector.impl.PlatformUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 3.服务/注册表探测策略 3.服务探测策略
 *
 * Windows: 通过注册表读取服务 ImagePath Linux: 读取 systemd service 文件和 SysV init.d 脚本
 *
 * 优先级: 30
 */
public class ServiceStrategy implements DetectionStrategy {

    private static final int PRIORITY = 30;

    // Windows: 注册表服务根路径
    private static final String WIN_REG_SERVICES_ROOT = "HKLM\\SYSTEM\\CurrentControlSet\\Services";
    // Windows: ImagePath 值提取正则（处理带引号和不带引号的情况）
    private static final Pattern WIN_IMAGE_PATH_PATTERN = Pattern.compile("ImagePath\\s+REG_(?:EXPAND_)?SZ\\s+(.+)", Pattern.CASE_INSENSITIVE);
    // Windows: 服务名提取（过滤系统服务）
    private static final Pattern WIN_SERVICE_NAME_PATTERN = Pattern.compile("^([A-Za-z0-9_-]+)$");

    // Linux systemd: ExecStart 提取正则
    private static final Pattern SYSTEMD_EXEC_PATTERN = Pattern.compile("^ExecStart\\s*=\\s*(.+)$");
    // Linux init.d: 常见路径变量提取正则
    private static final Pattern INITD_PATH_VAR_PATTERN = Pattern.compile(
            "^(?:CATALINA_HOME|NGINX_HOME|HTTPD_ROOT|INSTALL_DIR|BASEDIR)\\s*=\\s*[\"']?([^\"'\\s]+)[\"']?\\s*$",
            Pattern.CASE_INSENSITIVE);
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PathResult> detect(ApplicationProfile profile) {
        List<PathResult> results;
        if (PlatformUtil.isWindows()) {
            results = detectWindows(profile);
        } else {
            results = detectUnix(profile);
        }
        profile.deduplicate(results);
        return results;
    }
    
    // ==================== Windows 实现 ====================
    /**
     * Windows 探测：通过注册表查询服务信息
     */
    private List<PathResult> detectWindows(ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();
        List<String> serviceNames = findMatchingServices(profile);

        for (String serviceName : serviceNames) {
            Path exePath = queryServiceImagePath(serviceName);
            if (exePath == null) {
                continue;
            }

            // 从可执行文件路径推导安装根目录
            Path installDir = PathDerivationUtil.deriveInstallDir(exePath, profile);
            if (installDir != null) {
                results.add(new PathResult(installDir, 100, this, "win-service:" + serviceName + ", ImagePath:" + exePath));
            }
        }

        return results;
    }

    /**
     * 查询 Windows 所有服务，匹配应用特征
     */
    private List<String> findMatchingServices(ApplicationProfile profile) {
        List<String> matched = new ArrayList<>();
        List<String> identifiers = profile.getServiceIdentifiers();

        if (identifiers == null || identifiers.isEmpty()) {
            return matched;
        }

        // 使用 reg query 列出所有服务名
        String[] cmd = {"reg", "query", WIN_REG_SERVICES_ROOT};
        List<String> lines = PlatformUtil.exec(cmd);

        if (lines.isEmpty()) {
            return matched;
        }

        // 解析输出，每行格式: HKLM\SYSTEM\...\Services\服务名
        String prefix = WIN_REG_SERVICES_ROOT + "\\";
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith(prefix)) {
                continue;
            }

            String serviceName = line.substring(prefix.length()).trim();
            // 过滤无效服务名
            if (!WIN_SERVICE_NAME_PATTERN.matcher(serviceName).matches()) {
                continue;
            }

            // 模糊匹配服务名
            String lowerServiceName = serviceName.toLowerCase();
            for (String id : identifiers) {
                if (lowerServiceName.contains(id.toLowerCase())) {
                    matched.add(serviceName);
                    break;
                }
            }
        }

        return matched;
    }

    /**
     * 查询指定服务的 ImagePath 注册表值
     */
    private Path queryServiceImagePath(String serviceName) {
        String regPath = WIN_REG_SERVICES_ROOT + "\\" + serviceName;
        String[] cmd = {"reg", "query", regPath, "/v", "ImagePath"};
        List<String> lines = PlatformUtil.exec(cmd);
        // 解析 ImagePath 值
        for (String line : lines) {
            Matcher matcher = WIN_IMAGE_PATH_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                String rawPath = matcher.group(1).trim();
                String exePath = extractExecutableFromImagePath(rawPath);
                if (exePath != null) {
                    return Paths.get(exePath);
                }
            }
        }

        return null;
    }

    /**
     * 从 ImagePath 原始值中提取可执行文件路径 处理格式: "C:\path\to\httpd.exe" -k runservice C:\path\to\nginx.exe \SystemRoot\system32\svchost.exe
     */
    private String extractExecutableFromImagePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return null;
        }

        String path = rawPath.trim();

        // 情况1: 带引号的路径 "C:\Program Files\..."
        if (path.startsWith("\"")) {
            int endQuote = path.indexOf('"', 1);
            if (endQuote > 1) {
                return path.substring(1, endQuote);
            }
        }

        // 情况2: 不带引号，取第一个空格前的部分作为路径
        int firstSpace = path.indexOf(' ');
        if (firstSpace > 0) {
            String candidate = path.substring(0, firstSpace);
            // 验证是否像路径（包含 :\ 或 \）
            if (candidate.contains("\\") || candidate.contains(":/")) {
                return candidate;
            }
        }

        // 情况3: 整个字符串就是路径
        if (path.contains("\\") || path.contains("/")) {
            return path;
        }

        return null;
    }

    // ==================== Linux/Unix 实现 ====================
    /**
     * Unix 探测：systemd + SysV init.d
     */
    private List<PathResult> detectUnix(ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();

        // 1. 探测 systemd 服务
        results.addAll(detectSystemdServices(profile));

        // 2. 探测 SysV init.d 脚本
        results.addAll(detectInitdServices(profile));

        return results;
    }

    /**
     * 探测 systemd 服务
     */
    private List<PathResult> detectSystemdServices(ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();
        List<String> serviceNames = profile.getServiceIdentifiers();

        if (serviceNames == null || serviceNames.isEmpty()) {
            return results;
        }

        // systemd 服务文件搜索路径（按优先级）
        List<Path> systemdPaths = new ArrayList<>();
        systemdPaths.add(Paths.get("/etc/systemd/system"));
        systemdPaths.add(Paths.get("/lib/systemd/system"));
        systemdPaths.add(Paths.get("/usr/lib/systemd/system"));
        systemdPaths.add(Paths.get("/run/systemd/system"));

        for (Path systemdDir : systemdPaths) {
            if (!Files.isDirectory(systemdDir)) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(systemdDir)) {
                for (Path serviceFile : stream) {
                    String fileName = serviceFile.getFileName().toString();

                    // 只处理 .service 文件
                    if (!fileName.endsWith(".service")) {
                        continue;
                    }

                    // 匹配服务名
                    String baseName = fileName.substring(0, fileName.length() - 8);
                    if (!matchesServiceName(baseName, serviceNames)) {
                        continue;
                    }

                    // 解析服务文件
                    Path installDir = parseSystemdServiceFile(serviceFile, profile);
                    if (installDir != null) {
                        results.add(new PathResult(installDir, 100, this, "systemd:" + baseName + ", file:" + serviceFile));
                    }
                }
            } catch (IOException e) {
                // 无权限读取目录，跳过
            }
        }

        return results;
    }

    /**
     * 解析 systemd service 文件，提取安装路径
     */
    private Path parseSystemdServiceFile(Path serviceFile, ApplicationProfile profile) {
        try (BufferedReader reader = Files.newBufferedReader(serviceFile, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }

                Matcher matcher = SYSTEMD_EXEC_PATTERN.matcher(line);
                if (matcher.find()) {
                    String execLine = matcher.group(1).trim();

                    // ExecStart 可能包含前缀修饰符如 @, -, :
                    // 例如: ExecStart=-/usr/sbin/nginx
                    execLine = execLine.replaceAll("^[\\-:@+!]+", "");

                    // 提取可执行文件路径（第一个空格前的部分）
                    String exePath = extractFirstPath(execLine);
                    if (exePath != null) {
                        return PathDerivationUtil.deriveInstallDir(Paths.get(exePath), profile);
                    }
                }
            }
        } catch (IOException e) {
            // 读取失败，返回 null
        }

        return null;
    }

    /**
     * 探测 SysV init.d 脚本
     */
    private List<PathResult> detectInitdServices(ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();
        List<String> serviceNames = profile.getServiceIdentifiers();

        if (serviceNames == null || serviceNames.isEmpty()) {
            return results;
        }

        Path initdDir = Paths.get("/etc/init.d");
        if (!Files.isDirectory(initdDir)) {
            return results;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(initdDir)) {
            for (Path script : stream) {
                if (!Files.isRegularFile(script) || !Files.isExecutable(script)) {
                    continue;
                }

                String scriptName = script.getFileName().toString();

                // 匹配脚本名
                if (!matchesServiceName(scriptName, serviceNames)) {
                    continue;
                }

                // 解析 init.d 脚本中的路径变量
                Path installDir = parseInitdScript(script, profile);
                if (installDir != null) {// init.d 脚本中的变量可能不准确，置信度稍低
                    results.add(new PathResult(installDir, 50, this, "initd:" + scriptName + ", file:" + script));
                }
            }
        } catch (IOException e) {
            // 无权限，跳过
        }

        return results;
    }

    /**
     * 解析 SysV init.d 脚本，提取路径变量
     */
    private Path parseInitdScript(Path script, ApplicationProfile profile) {
        try (BufferedReader reader = Files.newBufferedReader(script, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 尝试匹配常见路径变量
                Matcher matcher = INITD_PATH_VAR_PATTERN.matcher(line);
                if (matcher.find()) {
                    String pathValue = matcher.group(1).trim();
                    // 处理变量引用，如 ${prefix}
                    pathValue = resolveVariableReference(pathValue, script);

                    Path candidate = Paths.get(pathValue);
                    if (PathDerivationUtil.hasConfirmatoryFiles(candidate, profile)) {
                        return candidate;
                    }

                    // 如果直接路径不对，尝试推导
                    Path derived = PathDerivationUtil.deriveInstallDir(candidate, profile);
                    if (derived != null) {
                        return derived;
                    }
                }

                // 特殊处理: 脚本中调用自身目录的情况
                // 如: CATALINA_HOME=$(cd "$(dirname "$0")/.." && pwd)
                // 这类动态赋值难以静态解析，依赖 confirmatory files 验证
            }
        } catch (IOException e) {
            // 读取失败
        }

        return null;
    }

    /**
     * 简单解析脚本中的变量引用 例如将 ${prefix} 或 $prefix 替换为实际值（如果能在脚本中找到定义）
     *
     * 注: 这是一个简化实现，复杂 shell 表达式无法完全解析
     */
    private String resolveVariableReference(String value, Path script) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 处理 ${VAR} 或 $VAR 形式
        if (!value.contains("$")) {
            return value;
        }

        // 尝试读取脚本中的变量定义
        try (BufferedReader reader = Files.newBufferedReader(script, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 匹配 prefix=/usr/local 或 prefix="/usr/local" 这类定义
                Pattern varDefPattern = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*[\"']?([^\"'\\s]+)[\"']?\\s*$");
                Matcher m = varDefPattern.matcher(line);
                if (m.find()) {
                    String varName = m.group(1);
                    String varValue = m.group(2);

                    // 替换 ${varName} 和 $varName
                    value = value.replace("${" + varName + "}", varValue);
                    value = value.replace("$" + varName, varValue);
                }
            }
        } catch (IOException e) {
            // 忽略
        }

        return value;
    }

    // ==================== 通用工具方法 ====================
    /**
     * 检查服务名是否匹配候选标识列表
     */
    private boolean matchesServiceName(String serviceName, List<String> identifiers) {
        if (serviceName == null || identifiers == null) {
            return false;
        }

        String lowerName = serviceName.toLowerCase();
        for (String id : identifiers) {
            if (lowerName.contains(id.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从命令行字符串中提取第一个路径 例如: /usr/sbin/nginx -g 'daemon on;' -> /usr/sbin/nginx
     */
    private String extractFirstPath(String cmdLine) {
        if (cmdLine == null || cmdLine.isEmpty()) {
            return null;
        }

        cmdLine = cmdLine.trim();

        // 情况1: 带引号的路径
        if (cmdLine.startsWith("'")) {
            int end = cmdLine.indexOf('\'', 1);
            if (end > 0) {
                return cmdLine.substring(1, end);
            }
        }
        if (cmdLine.startsWith("\"")) {
            int end = cmdLine.indexOf('"', 1);
            if (end > 0) {
                return cmdLine.substring(1, end);
            }
        }

        // 情况2: 取第一个空格前的部分
        int spaceIdx = cmdLine.indexOf(' ');
        if (spaceIdx > 0) {
            return cmdLine.substring(0, spaceIdx);
        }

        // 情况3: 整个字符串
        return cmdLine;
    }
}
