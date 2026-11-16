package qingzhou.app.nginx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qingzhou.api.AppContext;

/**
 * nginx 安装位置与运行时文件布局。
 *
 * 解析顺序：应用配置（path/nginxPath/home/prefix）→ 平台探测路径 → NGINX_HOME/NGINX_PREFIX
 * → 运行中进程 → PATH → 常见安装目录。配置优先，保证非标准安装目录也能纳管；
 * 进程与 PATH 兜底用于未显式配置的场景。
 */
final class NginxInstall {
    private static final int CONFIG_SCAN_BYTES = 1024 * 1024;
    private static final Pattern PID_DIRECTIVE =
            Pattern.compile("(?m)^\\s*pid\\s+([^;#\\s]+)\\s*;");
    private static final Pattern LOG_DIRECTIVE =
            Pattern.compile("(?m)^\\s*(?:error_log|access_log)\\s+([^;#\\s]+)");

    final Path prefix;
    final Path binary;
    final Path conf;
    final Path pidFile;
    final List<Path> logDirs;
    final String version;
    final String configureArguments;

    private NginxInstall(Path prefix, Path binary, Path conf, Path pidFile,
                         List<Path> logDirs, CompileInfo compileInfo) {
        this.prefix = prefix;
        this.binary = binary;
        this.conf = conf;
        this.pidFile = pidFile;
        this.logDirs = Collections.unmodifiableList(new ArrayList<>(logDirs));
        this.version = compileInfo == null ? null : compileInfo.version;
        this.configureArguments = compileInfo == null ? null : compileInfo.arguments;
    }

    static NginxInstall resolve(AppContext context) {
        // 引擎级自动发现会为每个命中目录各启动一个 bundle 实例，实例的探测路径是该实例的唯一身份，
        // 必须优先于共享的 app~...path，否则多实例会全部操作同一个目录。
        String detected = context.getDetectedPath();
        if (detected != null && !detected.trim().isEmpty()) {
            NginxInstall install = tryBuild(detected);
            if (install != null) return install;
            throw new NotFoundException("平台探测到的 nginx 路径不可用: " + detected);
        }

        Map<String, String> config = context.getAppConfig();
        List<String> configured = new ArrayList<>();
        addHint(configured, config.get("path"));
        addHint(configured, config.get("nginxPath"));
        addHint(configured, config.get("home"));
        addHint(configured, config.get("prefix"));
        addHint(configured, System.getenv("NGINX_HOME"));
        addHint(configured, System.getenv("NGINX_PREFIX"));

        // 显式配置（应用配置/平台探测/环境变量）一旦给出就只认它，解析失败直接报错：
        // 静默回落到进程/PATH/常见目录会把启停动作作用到另一套 nginx。
        if (!configured.isEmpty()) {
            for (String hint : configured) {
                NginxInstall install = tryBuild(hint);
                if (install != null) return install;
            }
            throw new NotFoundException("显式配置的 nginx 路径不可用: " + String.join(", ", configured)
                    + "。请修正 app~qingzhou-app-nginx.path，或移除该配置以启用自动探测");
        }

        List<NginxInstall> processInstalls = findProcessInstalls();
        if (processInstalls.size() > 1) {
            List<String> paths = new ArrayList<>();
            for (NginxInstall install : processInstalls) paths.add(absolute(install.prefix));
            throw new NotFoundException("检测到多个运行中的 nginx: " + String.join(", ", paths)
                    + "，请通过 app~qingzhou-app-nginx.path 显式指定要管理的实例");
        }
        if (processInstalls.size() == 1) return processInstalls.get(0);

        Path binary = findInPath();
        if (binary != null) {
            NginxInstall install = buildFromBinary(binary);
            if (install != null) return install;
        }
        for (Path dir : commonDirs()) {
            NginxInstall install = buildFromDirectory(dir);
            if (install != null) return install;
        }

        throw new NotFoundException("未找到可用的 nginx，请确认已安装 nginx，"
                + "或在轻舟配置文件中设置 app~qingzhou-app-nginx.path=<nginx 安装目录或可执行文件>");
    }

    static String absolute(Path path) {
        return path.toAbsolutePath().normalize().toString().replace(File.separatorChar, '/');
    }

    String display(Path path) {
        Path target = path.toAbsolutePath().normalize();
        Path base = prefix.toAbsolutePath().normalize();
        if (target.startsWith(base)) {
            return base.relativize(target).toString().replace(File.separatorChar, '/');
        }
        return absolute(target);
    }

    String displayLogDirs() {
        List<String> values = new ArrayList<>();
        for (Path dir : logDirs) values.add(display(dir));
        return String.join(", ", values);
    }

    /**
     * 只允许访问安装目录、主配置所在目录（以及可选的日志目录）内的真实文件，
     * 防止通过符号链接把编辑或读取引向系统任意文件。
     */
    boolean isAllowed(Path file, boolean includeLogs) {
        Path target = realPath(file);
        if (target == null) return false;

        Set<Path> roots = new LinkedHashSet<>();
        roots.add(prefix);
        Path confDir = conf.getParent();
        if (confDir != null) roots.add(confDir);
        if (includeLogs) roots.addAll(logDirs);

        for (Path root : roots) {
            Path realRoot = realPath(root);
            if (realRoot != null && target.startsWith(realRoot)) return true;
        }
        return false;
    }

    private static Path realPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return null;
        }
    }

    private static void addHint(List<String> hints, String value) {
        if (value == null) return;
        String hint = value.trim();
        if (hint.isEmpty()) return;
        if ((hint.startsWith("\"") && hint.endsWith("\""))
                || (hint.startsWith("'") && hint.endsWith("'"))) {
            hint = hint.substring(1, hint.length() - 1).trim();
        }
        if (!hint.isEmpty() && !hints.contains(hint)) hints.add(hint);
    }

    private static NginxInstall tryBuild(String hint) {
        Path path;
        try {
            path = Paths.get(hint).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return null;
        }
        if (Files.isDirectory(path)) return buildFromDirectory(path);
        if (!Files.isRegularFile(path)) return null;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".conf") ? buildFromConf(path) : buildFromBinary(path);
    }

    private static NginxInstall buildFromDirectory(Path dir) {
        if (!Files.isDirectory(dir)) return null;

        Path conf = dir.resolve("conf").resolve("nginx.conf");
        Path localBinary = findLocalBinary(dir);
        // 既没有 conf/nginx.conf 也没有目录内可执行文件的普通目录不得触发 PATH 回退，
        // 否则 path=/tmp 之类的笔误会被解析成机器上另一套 nginx。
        if (!Files.isRegularFile(conf) && localBinary == null) return null;

        Path binary = localBinary == null ? findInPath() : localBinary;
        CompileInfo info = binary == null ? null : compileInfo(binary);

        // 只有同时具备 conf/nginx.conf 与目录内可执行文件才视作完整安装目录；
        // 否则按“配置目录”处理，prefix 采用二进制编译期 prefix（如 Debian 的 /etc/nginx + /usr/sbin/nginx）。
        boolean isInstallDir = Files.isRegularFile(conf) && localBinary != null;
        if (!Files.isRegularFile(conf)) {
            if (info == null || info.confPath == null || !Files.isRegularFile(info.confPath)) return null;
            conf = info.confPath;
        }
        if (binary == null) {
            binary = findInPath();
            info = binary == null ? null : compileInfo(binary);
        }
        if (binary == null) return null;

        Path prefix = dir;
        if (!isInstallDir) {
            Path compiledPrefix = info == null ? null : info.prefixPath;
            if (compiledPrefix != null && Files.isDirectory(compiledPrefix)) prefix = compiledPrefix;
        }
        return create(prefix, binary, conf, info);
    }

    private static NginxInstall buildFromBinary(Path binary) {
        return buildFromBinary(binary, null, null);
    }

    private static NginxInstall buildFromBinary(Path binary, Path prefixHint, Path confHint) {
        if (binary == null || !Files.isRegularFile(binary)) return null;
        CompileInfo info = compileInfo(binary);

        Path localPrefix = defaultPrefix(binary);
        Path localConf = localPrefix == null ? null : localPrefix.resolve("conf").resolve("nginx.conf");

        Path prefix = prefixHint;
        Path conf = confHint;
        // 整目录搬迁后的安装包：二进制旁的 conf/nginx.conf 比编译期 prefix 更可信
        if ((prefix == null || !Files.isDirectory(prefix))
                && localConf != null && Files.isRegularFile(localConf)) {
            prefix = localPrefix;
            conf = localConf;
        }
        if (prefix == null || !Files.isDirectory(prefix)) prefix = info == null ? null : info.prefixPath;
        if (prefix == null || !Files.isDirectory(prefix)) prefix = localPrefix;
        if (prefix == null || !Files.isDirectory(prefix)) return null;

        // 显式 prefix 下的 conf/nginx.conf 优先于编译期 conf-path（搬迁场景两者可能不一致）
        if (conf == null || !Files.isRegularFile(conf)) conf = prefix.resolve("conf").resolve("nginx.conf");
        if (!Files.isRegularFile(conf)) conf = info == null ? null : info.confPath;
        if (conf == null || !Files.isRegularFile(conf)) return null;
        return create(prefix, binary, conf, info);
    }

    private static NginxInstall buildFromConf(Path conf) {
        if (conf == null || !Files.isRegularFile(conf)) return null;
        Path binary = findInPath();
        if (binary == null) return null;

        CompileInfo info = compileInfo(binary);
        Path prefix = info == null ? null : info.prefixPath;
        if (prefix == null || !Files.isDirectory(prefix)) prefix = defaultPrefix(binary);
        if (prefix == null) return null;
        return create(prefix, binary, conf, info);
    }

    private static NginxInstall create(Path prefix, Path binary, Path conf, CompileInfo info) {
        if (!Files.isRegularFile(binary) || !Files.isRegularFile(conf)) return null;

        Path pidFile = parsePid(conf, prefix);
        if (pidFile == null && info != null && info.pidPath != null) pidFile = info.pidPath;
        if (pidFile == null) pidFile = prefix.resolve("logs").resolve("nginx.pid");

        return new NginxInstall(prefix, binary, conf, pidFile,
                collectLogDirs(prefix, conf, info), info);
    }

    private static Path defaultPrefix(Path binary) {
        Path parent = binary.getParent();
        if (parent == null) return null;
        if (parent.getFileName() != null
                && "sbin".equalsIgnoreCase(parent.getFileName().toString())) {
            return parent.getParent();
        }
        return parent;
    }

    private static Path findLocalBinary(Path prefix) {
        Path[] candidates = {
                prefix.resolve("sbin").resolve("nginx"),
                prefix.resolve("sbin").resolve("nginx.exe"),
                prefix.resolve("bin").resolve("nginx"),
                prefix.resolve("bin").resolve("nginx.exe"),
                prefix.resolve("nginx.exe"),
                prefix.resolve("nginx")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) return candidate.toAbsolutePath().normalize();
        }
        return null;
    }

    private static Path findInPath() {
        String path = System.getenv("PATH");
        if (path == null) return null;
        String[] names = NginxCommand.isWindows()
                ? new String[]{"nginx.exe", "nginx"} : new String[]{"nginx"};
        for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
            if (dir.trim().isEmpty()) continue;
            for (String name : names) {
                Path candidate = Paths.get(dir.trim(), name);
                if (Files.isRegularFile(candidate)) return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static List<NginxInstall> findProcessInstalls() {
        List<NginxInstall> installs = new ArrayList<>();
        try {
            if (NginxCommand.isWindows()) {
                NginxCommand.Result result = NginxCommand.run(null, 8, "powershell", "-NoProfile",
                        "-NonInteractive", "-Command",
                        "Get-CimInstance Win32_Process -Filter \"Name='nginx.exe'\" |"
                                + " ForEach-Object { $_.ExecutablePath + '|' + $_.CommandLine }");
                if (!result.isSuccess()) return installs;
                for (String line : result.getOutput().split("\\R")) {
                    int split = line.indexOf('|');
                    String executable = split < 0 ? line : line.substring(0, split);
                    String commandLine = split < 0 ? "" : line.substring(split + 1);
                    addProcessInstall(installs, existingFile(executable), commandLine);
                }
                return installs;
            }

            NginxCommand.Result result = NginxCommand.run(null, 8, "ps", "-ww", "-e", "-o", "args=");
            if (!result.isSuccess()) return installs;
            Matcher matcher = Pattern.compile("nginx: master process\\s+(\\S+)(.*)").matcher(result.getOutput());
            while (matcher.find()) {
                addProcessInstall(installs, existingFile(matcher.group(1)), matcher.group(2));
            }
        } catch (RuntimeException ignored) {
            // 进程探测只是兜底，ps/powershell 不可用时继续走 PATH 与常见目录
        }
        return installs;
    }

    private static void addProcessInstall(List<NginxInstall> installs, Path binary, String commandLine) {
        NginxInstall install = fromProcess(binary, commandLine);
        if (install == null) return;
        for (NginxInstall existing : installs) {
            if (existing.prefix.equals(install.prefix) && existing.conf.equals(install.conf)) return;
        }
        installs.add(install);
    }

    private static NginxInstall fromProcess(Path binary, String commandLine) {
        if (binary == null) return null;
        String prefixValue = optionValue(commandLine, "-p");
        String confValue = optionValue(commandLine, "-c");
        Path prefix = absoluteOption(prefixValue);
        Path conf = null;
        if (confValue != null) {
            try {
                Path path = Paths.get(confValue);
                if (path.isAbsolute()) {
                    conf = path.normalize();
                } else if (prefix != null) {
                    conf = prefix.resolve(path).normalize();
                }
            } catch (InvalidPathException ignored) {
                // 非法路径按未提供处理，交给编译期 conf-path 兜底
            }
        }
        return buildFromBinary(binary, prefix, conf);
    }

    private static String optionValue(String commandLine, String option) {
        if (commandLine == null) return null;
        List<String> tokens = tokenize(commandLine);
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals(option)) {
                return i + 1 < tokens.size() ? tokens.get(i + 1) : null;
            }
            if (token.startsWith(option) && token.length() > option.length()) {
                return token.substring(option.length());
            }
        }
        return null;
    }

    // nginx 命令行只按空白分词，引号仅用于让含空格路径成为一个参数；反斜杠是 Windows 路径字符，不能当转义符
    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                else current.append(c);
            } else if (c == '\"' || c == '\'') {
                quote = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());
        return tokens;
    }

    private static Path absoluteOption(String value) {
        if (value == null) return null;
        try {
            Path path = Paths.get(value);
            return path.isAbsolute() ? path.normalize() : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static Path existingFile(String value) {
        if (value == null) return null;
        String path = value.trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).trim();
        }
        if (path.isEmpty()) return null;
        try {
            Path candidate = Paths.get(path).toAbsolutePath().normalize();
            return Files.isRegularFile(candidate) ? candidate : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static List<Path> commonDirs() {
        List<Path> dirs = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        if (NginxCommand.isWindows()) {
            addDir(dirs, userHome == null ? null : userHome + "\\nginx");
            addDir(dirs, "C:\\nginx");
            addDir(dirs, "C:\\Program Files\\nginx");
            addDir(dirs, "C:\\Program Files (x86)\\nginx");
            addDir(dirs, "C:\\ProgramData\\nginx");
            addDir(dirs, System.getenv("ProgramFiles") == null ? null : System.getenv("ProgramFiles") + "\\nginx");
            addDir(dirs, System.getenv("ProgramFiles(x86)") == null ? null : System.getenv("ProgramFiles(x86)") + "\\nginx");
            addDir(dirs, System.getenv("ProgramData") == null ? null : System.getenv("ProgramData") + "\\nginx");
        } else {
            addDir(dirs, "/usr/local");
            addDir(dirs, "/usr/local/nginx");
            addDir(dirs, "/opt/nginx");
            addDir(dirs, "/opt/homebrew");
            addDir(dirs, "/opt/homebrew/opt/nginx");
            addDir(dirs, "/usr/local/opt/nginx");
            addDir(dirs, "/usr/local/openresty/nginx");
            addDir(dirs, "/opt/openresty/nginx");
            addDir(dirs, "/usr/share/nginx");
            addDir(dirs, "/etc/nginx");
            addDir(dirs, userHome == null ? null : userHome + "/nginx");
            addDir(dirs, userHome == null ? null : userHome + "/opt/nginx");
        }
        return dirs;
    }

    private static void addDir(List<Path> dirs, String value) {
        if (value == null || value.trim().isEmpty()) return;
        try {
            Path dir = Paths.get(value).toAbsolutePath().normalize();
            if (Files.isDirectory(dir) && !dirs.contains(dir)) dirs.add(dir);
        } catch (InvalidPathException ignored) {
        }
    }

    private static Path parsePid(Path conf, Path prefix) {
        String text = readHead(conf, CONFIG_SCAN_BYTES);
        Matcher matcher = PID_DIRECTIVE.matcher(text);
        if (!matcher.find()) return null;
        String value = unquote(matcher.group(1));
        if (value.isEmpty() || value.contains("$")) return null;
        return resolve(prefix, value);
    }

    private static List<Path> collectLogDirs(Path prefix, Path conf, CompileInfo info) {
        Set<Path> dirs = new LinkedHashSet<>();
        dirs.add(prefix.resolve("logs"));
        addParentDirectory(dirs, info == null ? null : info.errorLogPath);
        addParentDirectory(dirs, info == null ? null : info.httpLogPath);

        String text = readHead(conf, CONFIG_SCAN_BYTES);
        Matcher matcher = LOG_DIRECTIVE.matcher(text);
        while (matcher.find()) {
            String value = unquote(matcher.group(1));
            if (value.isEmpty() || value.contains("$")
                    || "off".equalsIgnoreCase(value) || "stderr".equalsIgnoreCase(value)
                    || value.startsWith("syslog:") || value.startsWith("memory:")) {
                continue;
            }
            addParentDirectory(dirs, resolve(prefix, value));
        }
        return new ArrayList<>(dirs);
    }

    boolean isDaemonOff() {
        String text = readHead(conf, CONFIG_SCAN_BYTES);
        return Pattern.compile("(?m)^\\s*daemon\\s+off\\s*;").matcher(text).find();
    }

    private static void addParentDirectory(Set<Path> dirs, Path file) {
        if (file == null) return;
        Path parent = file.getParent();
        if (parent != null && Files.isDirectory(parent)) dirs.add(parent);
    }

    private static Path resolve(Path prefix, String value) {
        try {
            Path path = Paths.get(value);
            return (path.isAbsolute() ? path : prefix.resolve(path)).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static String unquote(String value) {
        String result = value == null ? "" : value.trim();
        if (result.length() >= 2) {
            char first = result.charAt(0);
            char last = result.charAt(result.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                result = result.substring(1, result.length() - 1).trim();
            }
        }
        return result;
    }

    private static String readHead(Path file, int maxBytes) {
        try (InputStream input = Files.newInputStream(file)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int remaining = maxBytes;
            int read;
            while (remaining > 0 && (read = input.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                output.write(buffer, 0, read);
                remaining -= read;
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 nginx 配置失败: " + file, e);
        }
    }

    private static CompileInfo compileInfo(Path binary) {
        if (binary == null || !Files.isRegularFile(binary)) return null;
        NginxCommand.Result result;
        try {
            result = NginxCommand.run(binary.getParent(), 8, binary.toString(), "-V");
        } catch (RuntimeException e) {
            return null;
        }
        if (!result.isSuccess()) return null;

        String text = result.getOutput();
        CompileInfo info = new CompileInfo();
        info.version = firstMatch(text, "nginx version:\\s*nginx/(\\S+)");
        info.arguments = substringAfter(text, "configure arguments:");
        info.prefixPath = pathArgument(text, "--prefix=");
        info.confPath = pathArgument(text, "--conf-path=");
        info.pidPath = pathArgument(text, "--pid-path=");
        info.errorLogPath = pathArgument(text, "--error-log-path=");
        info.httpLogPath = pathArgument(text, "--http-log-path=");
        return info;
    }

    private static Path pathArgument(String text, String name) {
        String arguments = substringAfter(text, "configure arguments:");
        if (arguments == null) return null;
        for (String token : tokenize(arguments)) {
            if (!token.startsWith(name)) continue;
            String value = unquote(token.substring(name.length()));
            if (value.isEmpty()) return null;
            try {
                return Paths.get(value).toAbsolutePath().normalize();
            } catch (InvalidPathException e) {
                return null;
            }
        }
        return null;
    }

    private static String firstMatch(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String substringAfter(String text, String token) {
        int index = text.indexOf(token);
        return index < 0 ? null : text.substring(index + token.length()).trim();
    }

    static final class NotFoundException extends IllegalStateException {
        NotFoundException(String message) {
            super(message);
        }
    }

    private static final class CompileInfo {
        String version;
        String arguments;
        Path prefixPath;
        Path confPath;
        Path pidPath;
        Path errorLogPath;
        Path httpLogPath;
    }
}
