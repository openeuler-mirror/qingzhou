package qingzhou.app.demo;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.api.action.*;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;

/**
 * 文件管理：演示文件上传 / 下载。
 *
 * <p>在 {@link AbstractCrudModel} 之上额外处理附件：通过 {@link #onAdd}/{@link #onUpdate} 钩子把上传的
 * 临时文件搬移到记录目录，并将 {@code attachment} 字段归一为文件名；{@link #delete} 时一并清理磁盘文件。
 */
@Model(code = "file-demo", order = 5,
        name = {"文件管理", "en:File Management"},
        info = {"演示文件上传与下载功能", "en:Demo file upload and download"},
        icon = "FolderOpened",
        menu = "advanced")
public class FileDemo extends AbstractCrudModel implements Page, Show, Add, Update, Delete, Download {
    private static final Map<String, Map<String, String>> db = new LinkedHashMap<>();

    public FileDemo() {
        if (!db.isEmpty()) return;

        seed("F001", "项目需求文档", "requirements.pdf", "项目第一期需求文档", "2026-01-10 09:00:00");
        seed("F002", "测试报告", "test-report.xlsx", "系统测试报告", "2026-02-15 14:30:00");
        seed("F003", "会议纪要", "meeting-notes.docx", "产品评审会议纪要", "2026-03-01 11:00:00");
        idCounter = 4;
    }

    private static void seed(String id, String name, String attachment, String desc, String created) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("id", id);
        f.put("name", name);
        f.put("attachment", attachment);
        f.put("description", desc);
        f.put("createdAt", created);
        db.put(id, f);
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @Override
    protected String genId(Map<String, String> d) {
        String id = "F" + nextSeq();
        d.put("id", id);
        d.put("createdAt", DateKit.now());
        return id;
    }

    @Override
    protected void onAdd(String id, Map<String, String> d) throws Exception {
        processUploadedFiles(id, d);
    }

    @Override
    protected void onUpdate(String id, Map<String, String> d) throws Exception {
        processUploadedFiles(id, d);
    }

    @Override
    public void delete(String id) {
        super.delete(id);
        // 一并删除记录目录下的全部物理文件
        File dir = new File(new File(getAppContext().getTempDir(), "file-demo"), id);
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
        dir.delete();
    }

    @Override
    public void start() {
        try {
            createSampleFile("F001", "requirements.pdf", "项目需求文档 - 示例文件内容\n");
            createSampleFile("F002", "test-report.xlsx", "测试项,结果,备注\n文件上传,通过,支持 multipart/form-data\n");
            createSampleFile("F003", "meeting-notes.docx", "产品评审会议纪要\n时间: 2026-03-01\n");
        } catch (Exception ignored) {
            // 初始化失败不阻断启动（示例文件缺失仅影响演示下载）
        }
    }

    private void createSampleFile(String recordId, String fileName, String content) throws Exception {
        File dir = new File(new File(getAppContext().getTempDir(), "file-demo"), recordId);
        dir.mkdirs();
        File f = new File(dir, fileName);
        if (!f.exists()) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(content);
            }
        }
    }

    /**
     * 覆盖框架默认 file 动作：仅去掉列表头入口，执行逻辑与默认实现一致（列出记录目录下的文件）。
     * id 会拼进基准目录，穿越字符在此拦截（与框架默认实现同一防线）。
     */
    @ModelAction(code = "file", icon = "Download", order = 3,
            name = {"下载", "en:Download"}, show = true, update = true, list = true,
            info = {"获取可下载的文件列表。", "en:Get the list of downloadable files."})
    public void files(Request request) {
        String id = request.getId();
        if (id != null && hasIllegalPath(id)) {
            request.getResponse().error("illegal file name");
            return;
        }

        Map<String, String> fileMap = new java.util.TreeMap<>();
        File base = file(id);
        if (base.isDirectory()) {
            File[] listFiles = base.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    if (f.isFile() && !f.getName().startsWith(".")) {
                        fileMap.put(f.getName(), String.valueOf(f.length()));
                    }
                }
            }
        }
        request.getResponse().data(fileMap);
    }

    @Override
    public File file(String id) {
        File baseDir = new File(getAppContext().getTempDir(), "file-demo");
        if (id != null && !id.isEmpty()) {
            File recordDir = new File(baseDir, id);
            if (!recordDir.exists()) recordDir.mkdirs();
            return recordDir;
        }
        if (!baseDir.exists()) baseDir.mkdirs();
        return baseDir;
    }

    /**
     * 把上传的临时文件搬移到记录目录，并将 attachment 归一为文件名（逗号分隔）。
     * 安全约束：只允许搬移实例临时目录（temp/...）内的文件，防止 attachment 指向任意本地路径被外带。
     */
    private void processUploadedFiles(String recordId, Map<String, String> data) throws Exception {
        String attachmentValue = data.get("attachment");
        if (attachmentValue == null || attachmentValue.isEmpty()) return;

        File recordDir = new File(new File(getAppContext().getTempDir(), "file-demo"), recordId);
        recordDir.mkdirs();

        File uploadBase = new File(getAppContext().getRoot(), "temp");
        String uploadBasePath = uploadBase.getCanonicalPath();

        List<String> fileNames = new ArrayList<>();
        for (String part : attachmentValue.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            File src = new File(part);
            if (src.exists() && src.isFile()) {
                if (!src.getCanonicalPath().startsWith(uploadBasePath + File.separator)) {
                    throw new IllegalArgumentException("invalid attachment path");
                }
                File dest = new File(recordDir, src.getName());
                if (dest.exists()) dest.delete();
                Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fileNames.add(src.getName());
            } else {
                fileNames.add(part); // 已有文件名（非路径），直接保留
            }
        }
        if (!fileNames.isEmpty()) data.put("attachment", String.join(",", fileNames));
    }

    private static boolean hasIllegalPath(String name) {
        return name.contains("..") || name.contains("/") || name.contains("\\");
    }

    @ModelField(id = true,
            name = {"文件ID", "en:File ID"},
            info = {"文件记录唯一标识", "en:File record unique identifier"},
            list = true, show = true, readonly = true)
    public String id;

    @ModelField(name = {"文件名称", "en:File Name"},
            info = {"文件名称", "en:File name"},
            list = true, show = true, add = true, update = true, search = true, required = true)
    public String name;

    @ModelField(name = {"附件", "en:Attachment"},
            info = {"上传的附件文件", "en:Uploaded attachment file"},
            list = true, show = true, add = true, update = true, input_type = InputType.file, group = "FileInfo")
    public String attachment;

    @ModelField(name = {"描述", "en:Description"},
            info = {"文件描述信息", "en:File description"},
            list = true, show = true, add = true, update = true, input_type = InputType.textarea, group = "FileInfo")
    public String description;

    @ModelField(name = {"创建时间", "en:Created"},
            info = {"记录创建时间", "en:Record creation time"},
            list = true, show = true, readonly = true, input_type = InputType.datetime)
    public String createdAt;
}
