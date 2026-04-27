package qingzhou.app.nginx;

import qingzhou.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nginx配置备份管理模型
 */
@Model(code = "backup", order = 4, icon = "FolderOpened", menu = "basic",
        name = {"配置备份", "en:Backup"}, info = {"管理Nginx配置文件的历史备份", "en:Manage Nginx configuration backups"})
public class NginxBackup extends qingzhou.api.ModelBase implements qingzhou.api.type.List, qingzhou.api.type.Show {

    @ModelField(id = true,
            name = {"备份文件名", "en:Backup File"},
            info = {"备份文件名称", "en:Backup file name"},
            list = true,
            show = true,
            readonly = true)
    public String backupFile;

    @ModelField(
            name = {"备份时间", "en:Backup Time"},
            info = {"备份创建时间", "en:Backup creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String backupTime;

    @ModelField(
            name = {"文件大小", "en:File Size"},
            info = {"备份文件大小", "en:Backup file size"},
            list = true,
            show = true,
            readonly = true)
    public String fileSize;

    /**
     * 列表查询：返回备份文件列表
     *
     * @param request
     * @param pageNum
     * @param pageSize
     * @param query
     * @param listFields
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        List<String[]> result = new ArrayList<>();
        List<Map<String, String>> allBackups = getAllBackups();

        // 过滤
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> backup : allBackups) {
            if (matchesQuery(backup, query)) {
                filtered.add(backup);
            }
        }

        // 分页
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> backup = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String fieldName = listFields[j];
                String fieldValue = backup.get(fieldName);
                data[j] = fieldValue != null ? fieldValue : "";
            }
            result.add(data);
        }

        return result;
    }

    /**
     * 检查备份文件是否存在
     *
     * @param id
     * @return
     */
    @Override
    public boolean contains(String id) {
        Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), id);
        return Files.exists(backupPath);
    }

    /**
     * 总数查询
     *
     * @param query
     * @return
     */
    @Override
    public int totalSize(Map<String, String> query) {
        try {
            java.util.List<Map<String, String>> allBackups = getAllBackups();
            int count = 0;
            for (Map<String, String> backup : allBackups) {
                if (matchesQuery(backup, query)) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 查看详情
     *
     * @param request
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public Map<String, String> show(Request request) throws Exception {
        String fileName = request.getId();
        Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), fileName);

        if (!Files.exists(backupPath)) {
            return null;
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("backupFile", fileName);

        // 读取文件内容
        String content = new String(Files.readAllBytes(backupPath));
        result.put("content", content);

        // 获取文件信息
        java.nio.file.attribute.BasicFileAttributes attrs = Files.readAttributes(backupPath, java.nio.file.attribute.BasicFileAttributes.class);
        LocalDateTime dateTime = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        result.put("backupTime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(dateTime));
        result.put("fileSize", formatFileSize(attrs.size()));

        return result;
    }

    /**
     * 创建备份（自定义操作）
     *
     * @throws java.io.IOException
     */
    public void createBackup() throws IOException {
        NginxConfigParser.backupConfig();
    }

    /**
     * 恢复备份（自定义操作）
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void restoreBackup(String fileName) throws IOException {
        Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), fileName);
        Path confPath = Paths.get(AppConfig.getNginxConfPath());

        if (!Files.exists(backupPath)) {
            throw new IllegalArgumentException("备份文件不存在: " + fileName);
        }

        // 先备份当前配置
        NginxConfigParser.backupConfig();

        // 恢复备份
        Files.copy(backupPath, confPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 删除备份（自定义操作）
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public void deleteBackup(String fileName) throws IOException {
        Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), fileName);
        if (Files.exists(backupPath)) {
            Files.delete(backupPath);
        }
    }

    /**
     * 获取所有备份
     */
    private java.util.List<Map<String, String>> getAllBackups() throws IOException {
        java.util.List<String> backupList = NginxConfigParser.getBackupList();
        java.util.List<Map<String, String>> result = new ArrayList<>();

        for (String fileName : backupList) {
            Path backupPath = Paths.get(AppConfig.getNginxBackupPath(), fileName);
            if (Files.exists(backupPath)) {
                Map<String, String> backupMap = new LinkedHashMap<>();
                backupMap.put("backupFile", fileName);

                java.nio.file.attribute.BasicFileAttributes attrs = Files.readAttributes(backupPath, java.nio.file.attribute.BasicFileAttributes.class);
                LocalDateTime dateTime = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                backupMap.put("backupTime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(dateTime));
                backupMap.put("fileSize", formatFileSize(attrs.size()));

                result.add(backupMap);
            }
        }

        return result;
    }

    /**
     * 查询匹配
     */
    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.trim().isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }
}
