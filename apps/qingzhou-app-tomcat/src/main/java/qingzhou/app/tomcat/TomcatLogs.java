package qingzhou.app.tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Show;

@Model(code = "logs", order = 4, icon = "Document",
        name = {"日志查看", "en:Log Viewer"},
        info = {"查看Tomcat日志文件", "en:View Tomcat log files"})
public class TomcatLogs extends TomcatModelBase implements qingzhou.api.type.List, Show {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"文件名", "en:File Name"},
            info = {"日志文件名", "en:Log file name"})
    public String fileName;

    @ModelField(list = true, show = true, readonly = true,
            name = {"修改时间", "en:Modified Time"},
            info = {"最后修改时间", "en:Last modified time"},
            input_type = InputType.datetime)
    public String modifiedTime;

    @ModelField(list = true, show = true, readonly = true,
            name = {"大小", "en:Size"},
            info = {"文件大小", "en:File size"})
    public String fileSize;

    @ModelField(list = true, show = true, readonly = true,
            name = {"类型", "en:Type"},
            info = {"日志类型", "en:Log type"})
    public String logType;

    @ModelField(show = true, readonly = true,
            name = {"内容", "en:Content"},
            info = {"日志内容", "en:Log content"},
            input_type = InputType.textarea)
    public String content;

    @ModelField(show = true, readonly = true,
            name = {"行数", "en:Lines"},
            info = {"日志总行数", "en:Total log lines"})
    public String lineCount;

    @Override
    public List<String[]> list(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) throws Exception {
        List<Map<String, String>> allLogs = listLogFiles();
        List<Map<String, String>> filtered = filterByQuery(allLogs, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            return filterByQuery(listLogFiles(), query).size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        return listLogFiles().stream().anyMatch(l -> l.get("fileName").equals(id));
    }

    @Override
    public Map<String, String> show(String id) {
        List<Map<String, String>> allLogs = listLogFiles();

        Map<String, String> logInfo = null;
        for (Map<String, String> log : allLogs) {
            if (log.get("fileName").equals(id)) {
                logInfo = log;
                break;
            }
        }

        if (logInfo == null) {
            return null;
        }

        String logPath = getLogPath(id);
        Path file = Paths.get(logPath);

        if (Files.exists(file)) {
            try {
                String fileContent = new String(Files.readAllBytes(file));
                long lines = fileContent.split("\n").length;

                logInfo.put("content", fileContent);
                logInfo.put("lineCount", String.valueOf(lines));
            } catch (IOException e) {
                logInfo.put("content", "无法读取日志文件: " + e.getMessage());
                logInfo.put("lineCount", "0");
            }
        } else {
            logInfo.put("content", "日志文件不存在");
            logInfo.put("lineCount", "0");
        }

        return logInfo;
    }

    private List<Map<String, String>> listLogFiles() {
        List<Map<String, String>> logs = new ArrayList<>();

        String logBasePath = getBaseLogsDir();
        if (logBasePath == null) {
            return logs;
        }
        Path logDir = Paths.get(logBasePath);

        if (!Files.exists(logDir)) {
            return logs;
        }

        try {
            Files.list(logDir)
                    .filter(p -> p.toString().endsWith(".log") || p.toString().endsWith(".txt"))
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            LocalDateTime ldt = attrs.lastModifiedTime().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

                            Map<String, String> log = new LinkedHashMap<>();
                            log.put("fileName", p.getFileName().toString());
                            log.put("modifiedTime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ldt));
                            log.put("fileSize", formatFileSize(attrs.size()));
                            log.put("logType", determineLogType(p.getFileName().toString()));
                            logs.add(log);
                        } catch (IOException e) {
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logs;
    }

    private String getLogPath(String fileName) {
        return resolvePath(getBaseLogsDir(), fileName).toString();
    }

    private String determineLogType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("catalina")) return "SYSTEM";
        if (lower.contains("localhost") && !lower.contains("access")) return "APP";
        if (lower.contains("access")) return "ACCESS";
        if (lower.contains("manager")) return "MANAGER";
        if (lower.contains("host-manager")) return "HOST";
        return "OTHER";
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}