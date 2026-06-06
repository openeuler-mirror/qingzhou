package qingzhou.detector.impl.strategy;

import qingzhou.detector.ApplicationProfile;
import qingzhou.detector.DetectionStrategy;
import qingzhou.detector.PathResult;
import qingzhou.detector.impl.PathDerivationUtil;
import qingzhou.detector.impl.PlatformUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2. 进程信息反推策略
 *
 * 通过枚举系统进程，匹配应用进程标识，获取进程可执行文件路径， 进而回溯推导安装根目录。
 * 特殊处理： - 对于 Java/Python 等解释器启动的应用（如 Tomcat），若从可执行文件路径 无法推导，则自动降级为从进程命令行参数中提取路径（复用 Profile 的正则模板）。
 * 跨平台支持： - Windows: tasklist 枚举 + wmic 获取 ExecutablePath - Linux: /proc 伪文件系统读取 comm、exe、cmdline - macOS: ps 枚举 + lsof/ps 获取可执行路径
 *
 * 优先级: 20
 */
public class ProcessStrategy implements DetectionStrategy {

    private static final int PRIORITY = 20;

    // 常见解释器进程名，需要额外读取完整命令行进行匹配
    private static final List<String> INTERPRETERS = Arrays.asList("java", "python", "python3", "python2", "ruby", "perl", "bash", "sh", "cmd", "powershell");

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PathResult> detect(ApplicationProfile profile) {
        List<PathResult> results = new ArrayList<>();

        List<ProcessInfo> processes = listAllProcesses();
        Set<String> processedPids = new HashSet<>();
        for (ProcessInfo proc : processes) {
            if (!processedPids.contains(proc.pid) && matchesProcess(proc, profile)) {
                Path installDir = null;
                String derivedFrom = null;

                // 1. 尝试从进程可执行文件路径推导
                Path exePath = Paths.get(proc.executablePath);
                if (exePath != null) {
                    installDir = PathDerivationUtil.deriveInstallDir(exePath, profile);
                    if (installDir != null) {
                        derivedFrom = "pid:" + proc.pid + ", exe:" + exePath;
                    }
                }

                // 2. 若失败且为解释器进程，从命令行参数中提取路径
                if (installDir == null) {
                    try {
                        installDir = PathDerivationUtil.deriveInstallDir(Paths.get(profile.extractPathByCmdArgs(proc.cmdline)), profile);
                        if (installDir != null) {
                            derivedFrom = "pid:" + proc.pid + ", exe:" + exePath;
                        } else {
                            installDir = PathDerivationUtil.deriveInstallDir(Paths.get(PlatformUtil.locateExecutable(proc.procName)), profile);
                            if (installDir != null) {
                                derivedFrom = "pid:" + proc.pid + ", procName:" + proc.procName;
                            }
                        }
                    } catch(Exception e){}
                }

                if (installDir != null) {
                    results.add(new PathResult(installDir, 100, this, derivedFrom));
                    processedPids.add(proc.pid);
                }
            }
        }

        profile.deduplicate(results);
        return results;
    }

    // ==================== 进程匹配 ====================
    /**
     * 判断进程是否匹配应用特征
     *
     * 1. 先匹配进程名（comm / Image Name） 2. 若进程名不匹配但属于解释器，再读取完整命令行参数匹配
     */
    private boolean matchesProcess(ProcessInfo proc, ApplicationProfile profile) {
        List<String> identifiers = profile.getProcessIdentifiers();
        if (identifiers == null || identifiers.isEmpty()) {
            return false;
        }

        String comm = proc.procName != null ? proc.procName.toLowerCase() : "";

        // 1. 进程名直接包含匹配
        for (String id : identifiers) {
            if (id != null && comm.contains(id.toLowerCase())) {
                return true;
            }
        }

        // 2. 解释器进程，读取完整命令行二次匹配
        if (isInterpreter(comm)) {
            String args = proc.cmdline;
            if (args != null) {
                String lowerArgs = args.toLowerCase();
                for (String id : identifiers) {
                    if (id != null && lowerArgs.contains(id.toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 判断进程名是否为解释器
     */
    private boolean isInterpreter(String comm) {
        if (comm == null) {
            return false;
        }
        String lower = comm.toLowerCase();
        for (String interp : INTERPRETERS) {
            if (lower.equals(interp) || lower.equals(interp + ".exe") || lower.endsWith("/" + interp) || lower.endsWith("\\" + interp)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 进程枚举 ====================
    /**
     * 枚举所有进程（分平台）
     */
    private List<ProcessInfo> listAllProcesses() {
        if (PlatformUtil.isWindows()) {
            return listAllProcessesWindows();
        } else if (PlatformUtil.isLinux()) {
            return listAllProcessesLinux();
        } else if (PlatformUtil.isMac()) {
            return listAllProcessesMac();
        }
        return Collections.emptyList();
    }

    // ---------- Windows ----------
    private List<ProcessInfo> listAllProcessesWindows() {
        List<ProcessInfo> list = new ArrayList<>();
        // 字段顺序要保证 ExecutablePath 和 ProcessId 在最后，因为 CommandLine 可能含逗号
        // wmic /format:csv 输出列：ComputerName,CommandLine,Name,ProcessId
        String[] fields = {"CommandLine", "ExecutablePath", "Name", "ProcessId"};
        List<String> rawLines = PlatformUtil.exec("wmic", "process", "get", String.join(",", fields), "/format:csv");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        if (lines.size() >= 2) {
            lines.remove(0);
            for (String line : lines) {
                // 除 CommandLine(可能多段)外, Name, ProcessId, ExecutablePath 均不包含逗号。
                int i0 = line.indexOf(','), i2 = line.lastIndexOf(',', line.lastIndexOf(',') - 1), i1 = line.lastIndexOf(',', i2 - 1);
                String cmdLine = line.substring(i0 + 1, i1).trim();
                String pid = line.substring(line.lastIndexOf(",") + 1).trim();
                String name = line.substring(i2 + 1, line.lastIndexOf(',')).trim();
                String exePath = line.substring(i1 + 1, i2).trim();
                if (cmdLine.startsWith("\\??\\")) {
                    cmdLine = cmdLine.substring("\\??\\".length());
                }
                list.add(new ProcessInfo(pid, name, exePath, cmdLine));
            }
        }
        return list;
    }

    /**
     * Linux: 读取 /proc 伪文件系统
     */
    private List<ProcessInfo> listAllProcessesLinux() {
        List<ProcessInfo> list = new ArrayList<>();
        Path procDir = Paths.get("/proc");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(procDir)) {
            for (Path entry : stream) {
                String pid = entry.getFileName().toString();
                if (!pid.matches("\\d+") || !Files.exists(entry.resolve("comm"))) {
                    continue;
                }

                try {
                    String procName = new String(Files.readAllBytes(Paths.get("/proc/" + pid + "/comm")), Charset.defaultCharset()).trim();
                    String executablePath = Files.readSymbolicLink(Paths.get("/proc/" + pid + "/exe")).toString();
                    if (!procName.isEmpty()) {
                        String cmdLine = "";
                        byte[] b = Files.readAllBytes(Paths.get(entry + "/cmdline"));
                        for (int i = 0; i < b.length; i++) {
                            if (b[i] == 0) {
                                b[i] = ' '; // \0 → 空格，拼成完整命令行
                            }
                        }
                        cmdLine = new String(b, "UTF-8").trim();
                        list.add(new ProcessInfo(pid, procName, executablePath, cmdLine));
                    }
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
            // 忽略无权限或 /proc 不可读
        }
        return list;
    }

    /**
     * macOS: ps -e -o pid,comm=
     */
    private List<ProcessInfo> listAllProcessesMac() {
        List<ProcessInfo> list = new ArrayList<>();
        List<String> lines = PlatformUtil.exec(new String[]{"ps", "-eo", "pid=,comm=,args="});
        Pattern pattern = Pattern.compile("^\\s*(\\d+)\\s+(\\S+)\\s*(.*)$");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String pid = m.group(1);
                    String procName = m.group(2);
                    String cmdLine = m.group(3).trim();

                    String path;
                    String args;
                    if (cmdLine.isEmpty()) {
                        path = "";
                        args = "";
                    } else if (cmdLine.startsWith("'") || cmdLine.startsWith("\"")) {
                        // 处理被引号包裹的路径
                        char quote = cmdLine.charAt(0);
                        int end = cmdLine.indexOf(quote, 1);
                        if (end > 0) {
                            path = cmdLine.substring(1, end);
                            args = cmdLine.substring(end + 1).trim();
                        } else {
                            path = cmdLine;
                            args = "";
                        }
                    } else {
                        // 没有引号时，以第一个空格分割路径和参数
                        int space = cmdLine.indexOf(' ');
                        if (space > 0) {
                            path = cmdLine.substring(0, space);
                            args = cmdLine.substring(space + 1).trim();
                        } else {
                            path = cmdLine;
                            args = "";
                        }
                    }
                    list.add(new ProcessInfo(pid, procName, path, args));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // ==================== 内部类 ====================
    /**
     * 进程信息封装
     */
    static class ProcessInfo {

        String pid;             // 进程 ID
        String procName;        // 进程名，如 nginx.exe、java.exe、java、httpd 等
        String executablePath;  // 可执行文件完整路径，如 C:\nginx\nginx.exe，可能为 null
        String cmdline;         // 完整命令行，可能为 null

        ProcessInfo(String pid, String procName, String executablePath, String cmdline) {
            this.pid = pid;
            this.procName = procName;
            this.executablePath = executablePath;
            this.cmdline = cmdline;
        }
    }
}
