package qingzhou.app.demo;

import java.io.File;
import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.DownloadFile;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;

/**
 * 演示文件上传和下载功能
 * - 支持通过表单上传附件（multipart/form-data）
 * - 支持通过 files/download action 下载文件
 * - 下载按钮在列表头、表格行、详情页、编辑页均显示（覆盖全场景）
 */
@Model(code = "file-demo", order = 5,
        name = {"文件管理", "en:File Management"},
        info = {"演示文件上传与下载功能", "en:Demo file upload and download"},
        icon = "FolderOpened",
        menu = "advanced")
public class FileDemo extends qingzhou.api.ModelBase implements List, Show, Add, Update, Delete, DownloadFile {
    private static final Map<String, Map<String, String>> db = new LinkedHashMap<>();
    private static int idCounter = 1;

    @Override
    public File parent(Request request) throws Exception {
        // 根据记录 ID 返回对应的文件目录
        String recordId = request.getId();
        File baseDir = new File(getAppContext().getTemp(), "file-demo");
        
        if (recordId != null && !recordId.isEmpty()) {
            // 有 ID 时，返回该记录对应的文件目录
            File recordDir = new File(baseDir, recordId);
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }
            return recordDir;
        }
        
        // 无 ID 时（如列表头下载），返回基础目录
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return baseDir;
    }
    
    public FileDemo() {
        if (!db.isEmpty()) return;

        // 初始化一些示例数据（attachment 存储纯文件名，对应 parent() 返回目录下的文件）
        Map<String, String> f1 = new LinkedHashMap<>();
        f1.put("id", "F001");
        f1.put("name", "项目需求文档");
        f1.put("attachment", "requirements.pdf");
        f1.put("description", "项目第一期需求文档");
        f1.put("createdAt", "2026-01-10 09:00:00");
        db.put(f1.get("id"), f1);

        Map<String, String> f2 = new LinkedHashMap<>();
        f2.put("id", "F002");
        f2.put("name", "测试报告");
        f2.put("attachment", "test-report.xlsx");
        f2.put("description", "系统测试报告");
        f2.put("createdAt", "2026-02-15 14:30:00");
        db.put(f2.get("id"), f2);

        Map<String, String> f3 = new LinkedHashMap<>();
        f3.put("id", "F003");
        f3.put("name", "会议纪要");
        f3.put("attachment", "meeting-notes.docx");
        f3.put("description", "产品评审会议纪要");
        f3.put("createdAt", "2026-03-01 11:00:00");
        db.put(f3.get("id"), f3);

        idCounter = 4;
    }
    
    @Override
    public void start() {
        // 在 start() 中初始化示例文件，此时 getAppContext() 已可用
        initSampleFiles();
    }
    
    private void initSampleFiles() {
        try {
            createSampleFilesForRecord("F001", "requirements.pdf", "项目需求文档 - 示例文件内容\n");
            createSampleFilesForRecord("F002", "test-report.xlsx", "测试项,结果,备注\n文件上传,通过,支持 multipart/form-data\n");
            createSampleFilesForRecord("F003", "meeting-notes.docx", "产品评审会议纪要\n时间: 2026-03-01\n");
        } catch (Exception e) {
            // 忽略初始化错误
        }
    }
    
    private void createSampleFilesForRecord(String recordId, String fileName, String content) throws Exception {
        File recordDir = new File(new File(getAppContext().getTemp(), "file-demo"), recordId);
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }
        File sampleFile = new File(recordDir, fileName);
        if (!sampleFile.exists()) {
            try (java.io.FileWriter fw = new java.io.FileWriter(sampleFile)) {
                fw.write(content);
            }
        }
    }

    @ModelField(id = true,
            name = {"文件ID", "en:File ID"},
            info = {"文件记录唯一标识", "en:File record unique identifier"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"文件名称", "en:File Name"},
            info = {"文件名称", "en:File name"},
            list = true,
            show = true,
            add = true,
            update = true,
            search = true,
            required = true)
    public String name;

    @ModelField(
            name = {"附件", "en:Attachment"},
            info = {"上传的附件文件", "en:Uploaded attachment file"},
            list = true,
            show = true,
            add = true,
            update = true,
            input_type = InputType.file,
            group = {"文件信息", "en:File Info"})
    public String attachment;

    @ModelField(
            name = {"描述", "en:Description"},
            info = {"文件描述信息", "en:File description"},
            list = true,
            show = true,
            add = true,
            update = true,
            input_type = InputType.textarea,
            group = {"文件信息", "en:File Info"})
    public String description;

    @ModelField(
            name = {"创建时间", "en:Created"},
            info = {"记录创建时间", "en:Record creation time"},
            list = true,
            show = true,
            readonly = true,
            input_type = InputType.datetime)
    public String createdAt;

    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> file : db.values()) {
            if (matchesQuery(file, query)) {
                filtered.add(file);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> f = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = f.get(listFields[j]);
                data[j] = value != null ? value : "";
            }
            result.add(data);
        }

        return result;
    }

    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        int count = 0;
        for (Map<String, String> file : db.values()) {
            if (matchesQuery(file, query)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean contains(String id) {
        return db.containsKey(id);
    }

    @Override
    public Map<String, String> show(Request request) {
        return db.get(request.getId());
    }

    @Override
    public void add(Request request, Map<String, String> data) throws Exception {
        String newId = "F" + String.format("%03d", idCounter++);
        data.put("id", newId);
        data.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        
        // 处理文件上传：将上传的文件移动到记录对应的目录
        processUploadedFiles(newId, data);
        
        db.put(newId, new LinkedHashMap<>(data));
    }

    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String id = request.getId();
        if (db.containsKey(id)) {
            // 处理文件上传：将上传的文件移动到记录对应的目录
            processUploadedFiles(id, data);
            
            Map<String, String> existing = db.get(id);
            existing.putAll(data);
            existing.put("id", id);
        }
    }
    
    /**
     * 处理上传的文件：将后端临时目录中的文件移动到记录对应的目录，
     * 并将 attachment 字段更新为文件名（而非路径）
     */
    private void processUploadedFiles(String recordId, Map<String, String> data) throws Exception {
        String attachmentValue = data.get("attachment");
        if (attachmentValue == null || attachmentValue.isEmpty()) {
            return;
        }
        
        File recordDir = new File(new File(getAppContext().getTemp(), "file-demo"), recordId);
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }
        
        // attachment 可能是逗号分隔的混合值：已有文件名 + 临时文件路径
        String[] parts = attachmentValue.split(",");
        java.util.List<String> fileNames = new ArrayList<>();
        
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            File srcFile = new File(part);
            if (srcFile.exists() && srcFile.isFile()) {
                // 临时文件路径：移动到记录目录
                String fileName = srcFile.getName();
                File destFile = new File(recordDir, fileName);
                
                if (destFile.exists()) {
                    destFile.delete();
                }
                
                java.nio.file.Files.move(srcFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileNames.add(fileName);
                
                File parentDir = srcFile.getParentFile();
                if (parentDir != null && parentDir.isDirectory()) {
                    File[] remaining = parentDir.listFiles();
                    if (remaining != null && remaining.length == 0) {
                        parentDir.delete();
                    }
                }
            } else {
                // 已有文件名（不是有效路径），直接保留
                fileNames.add(part);
            }
        }
        
        // 将 attachment 字段更新为文件名（逗号分隔），而非路径
        if (!fileNames.isEmpty()) {
            data.put("attachment", String.join(",", fileNames));
        }
    }

    @Override
    public void delete(String id) throws Exception {
        db.remove(id);
    }
}
