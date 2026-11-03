package qingzhou.ai.skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AppBuilderProjects {
    private final Path instance;
    private final Path library;
    private final String version;
    private final String pomTemplate;

    AppBuilderProjects(Path instance, Path library, String version, String pomTemplate) {
        this.instance = instance;
        this.library = library;
        this.version = version;
        this.pomTemplate = pomTemplate;
    }

    synchronized String create(String app) throws IOException {
        Path project = project(app);
        Path pom = checked(project, "pom.xml");
        if (!Files.exists(pom)) {
            Files.createDirectories(checked(project, "src/main/java"));
            dependency(project, "qingzhou-api", "modules");
            dependency(project, "qingzhou-json", "components");
            String xml = pomTemplate.replace("@APP@", app).replace("@QINGZHOU_VERSION@", version);
            Files.write(pom, xml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        }
        return "工程目录：" + project + "\n已有源码及 POM 保持不变。可人工修改后在此目录执行 mvn clean package。" +
                "\n用 app_builder_source（不提供 path）列出已有源码。";
    }

    synchronized String source(String app, String path, String content, String offset) throws IOException {
        Path project = existingProject(app);
        if (path == null || path.isEmpty()) {
            if (content != null) throw new IllegalArgumentException("保存源码必须提供 path");
            Path sourceRoot = checked(project, "src/main/java");
            try (Stream<Path> stream = Files.walk(sourceRoot)) {
                String names = stream.filter(file -> file.toString().endsWith(".java")).sorted()
                        .map(file -> project.relativize(file).toString().replace('\\', '/'))
                        .collect(Collectors.joining("\n"));
                return AppBuilderSkill.page(names, offset);
            }
        }
        if (!path.matches("src/main/java/(?:[A-Za-z_$][A-Za-z0-9_$]*/)*[A-Za-z_$][A-Za-z0-9_$]*\\.java")) {
            throw new IllegalArgumentException("仅支持 src/main/java/ 下的相对 Java 源码路径，使用 / 分隔");
        }
        Path file = checked(project, path);
        if (content == null) return AppBuilderSkill.page(AppBuilderSkill.read(file), offset);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return "已保存：" + path;
    }

    synchronized String build(String app) throws IOException, InterruptedException {
        Path project = existingProject(app);
        String jarName = "qingzhou-app-" + app + ".jar";
        Path destination = checked(instance, "apps/" + jarName);
        if (Files.exists(destination)) return "未构建：apps 已有 " + jarName + "，请与用户决定使用新应用标识或人工处理旧包。";
        Path javaHome = Paths.get(System.getProperty("java.home"));
        if (javaHome.getFileName().toString().equalsIgnoreCase("jre")) javaHome = javaHome.getParent();
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if (!"1.8".equals(System.getProperty("java.specification.version")) ||
                !Files.isRegularFile(javaHome.resolve(isWindows ? "bin/javac.exe" : "bin/javac"))) {
            return "未构建：请使用完整 JDK 1.8 启动轻舟，并将支持 Java 8 的 Maven 加入 PATH。工程已保留：" + project;
        }
        // Maven clean 会删除 target；构建前拒绝工程中的越界链接，包括人工编辑产生的链接。
        try (Stream<Path> stream = Files.walk(project)) {
            List<Path> files = stream.collect(Collectors.toList());
            if (files.stream().noneMatch(file -> file.startsWith(project.resolve("src/main/java")) &&
                    Files.isRegularFile(file) && file.toString().endsWith(".java"))) return "未构建：工程内没有 Java 源码。";
            for (Path file : files) {
                checked(project, project.relativize(file).toString());
            }
        }
        Path log = checked(project, "build.log");
        Files.deleteIfExists(log);
        // JDK 8 没有 ProcessHandle；Windows 由启动器掌握 PID，超时结束 Maven 整个进程树。
        List<String> command = isWindows ? Arrays.asList("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                "$ErrorActionPreference='Stop'; " +
                        "$build=Start-Process cmd.exe -ArgumentList '/d /c \"mvn.cmd -B clean package > build.log 2>&1\"' " +
                        "-WorkingDirectory (Get-Location).Path -WindowStyle Hidden -PassThru; $null=$build.Handle; " +
                        "try { if ($build.WaitForExit(120000)) { $exitCode=$build.ExitCode } else { $exitCode=124 } } " +
                        "finally { if (-not $build.HasExited) { taskkill.exe /PID $build.Id /T /F | Out-Null } }; exit $exitCode") :
                Arrays.asList("mvn", "-B", "clean", "package");
        Path output = isWindows ? checked(project, "launcher.log") : log;
        ProcessBuilder builder = new ProcessBuilder(command).directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().put("JAVA_HOME", javaHome.toString());
        builder.environment().put("MAVEN_OPTS", builder.environment().getOrDefault("MAVEN_OPTS", "") + " -Dfile.encoding=UTF-8");
        Process compilation;
        try {
            compilation = builder.start();
        } catch (IOException e) {
            return "未构建：无法启动 Maven，请检查 PATH。工程保留在：" + project;
        }
        InterruptedException interruption = null;
        try {
            compilation.getOutputStream().close();
            if (!compilation.waitFor(isWindows ? 130 : 120, TimeUnit.SECONDS) || compilation.exitValue() == 124) {
                return "构建超时，未放置 JAR。请与用户确认网络、Maven 配置后重试。工程：" + project;
            }
            if (compilation.exitValue() != 0) {
                Path failureLog = Files.exists(log) ? log : output;
                return "构建失败，未放置 JAR。工程：" + project + "\n完整日志：" + failureLog + "\n" + diagnostics(failureLog);
            }
        } catch (InterruptedException e) {
            interruption = e;
            throw e;
        } finally {
            try {
                // 请求中断也等 Windows 启动器完成进程树清理，避免重建与旧 Maven 并行写 target。
                if (isWindows && compilation.isAlive()) compilation.waitFor(130, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interruption = e;
            } finally {
                if (compilation.isAlive()) compilation.destroyForcibly();
                // 终止请求不保证进程立即退出；确认结束后才释放构建锁。
                while (compilation.isAlive()) {
                    try {
                        compilation.waitFor();
                    } catch (InterruptedException e) {
                        interruption = e;
                    }
                }
                if (interruption != null) {
                    Thread.currentThread().interrupt();
                    throw interruption;
                }
            }
        }
        Path jar = checked(project, "target/" + jarName);
        if (!Files.isRegularFile(jar)) return "构建结束，但未找到预期产物：" + jar + "。请检查 POM 的 finalName。未放置应用。";
        Files.createDirectories(destination.getParent());
        Path staged = Files.createTempFile(destination.getParent(), "app-builder-", ".part");
        try {
            Files.copy(jar, staged, StandardCopyOption.REPLACE_EXISTING);
            Files.move(staged, destination);
        } finally {
            Files.deleteIfExists(staged);
        }
        return "构建成功，JAR 已放入：" + destination + "\n应用标识应为：" + app +
                "\n工程保留在：" + project + "\n请按运行说明配置服务地址和凭据，然后手动重启轻舟查看应用；当前尚未加载或验证运行结果。";
    }

    private Path project(String app) throws IOException {
        if (app == null || !app.matches("[a-z][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("应用标识须为小写字母开头的 1 至 64 位小写字母、数字或连字符");
        }
        return checked(instance, "temp/app-builder/qingzhou-app-" + app);
    }

    private Path existingProject(String app) throws IOException {
        Path project = project(app);
        if (!Files.isRegularFile(checked(project, "pom.xml"))) throw new IOException("请先用 app_builder_project 创建工程");
        return project;
    }

    private void dependency(Path project, String artifact, String directory) throws IOException {
        String relative = ".qingzhou-repository/qingzhou/" + artifact + "/" + version + "/" + artifact + "-" + version;
        Path jar = checked(project, relative + ".jar");
        Path pomFile = checked(project, relative + ".pom");
        Path repository = jar.getParent();
        Files.createDirectories(repository);
        Files.copy(library.resolve(directory).resolve(artifact + ".jar"), jar,
                StandardCopyOption.REPLACE_EXISTING);
        String pom = "<project><modelVersion>4.0.0</modelVersion><groupId>qingzhou</groupId><artifactId>" + artifact +
                "</artifactId><version>" + version + "</version></project>";
        Files.write(pomFile, pom.getBytes(StandardCharsets.UTF_8));
    }

    private static Path checked(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) throw new IOException("路径超出工程目录");
        for (Path parent = path; parent != null && parent.startsWith(root); parent = parent.getParent()) {
            if (Files.isSymbolicLink(parent) || Files.exists(parent) && !parent.toRealPath().startsWith(root)) {
                throw new IOException("工程路径不允许使用越界链接：" + parent);
            }
        }
        return path;
    }

    private static String diagnostics(Path log) throws IOException {
        StringBuilder errors = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[ERROR]")) errors.append(line).append('\n');
                if (errors.length() >= 1300) break;
            }
        }
        return errors.length() == 0 ? "请人工查看完整构建日志。" : errors.substring(0, Math.min(1300, errors.length()));
    }
}
