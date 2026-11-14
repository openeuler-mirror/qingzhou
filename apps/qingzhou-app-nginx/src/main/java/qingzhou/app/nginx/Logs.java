package qingzhou.app.nginx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "logs", order = 3, icon = "Tickets",
        name = {"日志查看", "en:Logs"},
        info = {"查看 nginx access/error 日志，大文件只读取尾部",
                "en:View nginx access/error logs; large files are tailed"})
public class Logs extends NginxModelBase implements Page, Show {

    @ModelField(id = true, list = true, search = true, readonly = true,
            name = {"文件", "en:File"},
            info = {"日志文件名；不同目录重名时附加路径哈希，避免 ID 冲突", "en:Log file name; hashed when duplicated"})
    public String name;

    @ModelField(list = true, search = true, readonly = true,
            name = {"目录", "en:Directory"},
            info = {"文件所在目录", "en:Directory containing the file"})
    public String dir;

    @ModelField(list = true, readonly = true,
            name = {"大小", "en:Size"},
            info = {"文件大小", "en:File size"})
    public String size;

    @ModelField(list = true, readonly = true, input_type = InputType.datetime,
            name = {"修改时间", "en:Modified"},
            info = {"最后修改时间", "en:Last modified time"})
    public String modified;

    @ModelField(list = true, readonly = true,
            name = {"类型", "en:Type"},
            info = {"按文件名归类：error=错误日志，access=访问日志，其他=未识别", "en:Error, access or other"})
    public String type;

    @ModelField(show = true, readonly = true,
            name = {"路径", "en:Path"},
            info = {"文件完整路径", "en:Full file path"})
    public String path;

    @ModelField(show = true, readonly = true,
            name = {"行数", "en:Lines"},
            info = {"当前展示内容的行数", "en:Number of lines in the displayed content"})
    public String lines;

    @ModelField(show = true, readonly = true, input_type = InputType.textarea,
            name = {"内容", "en:Content"},
            info = {"最多展示文件尾部 2 MB", "en:At most the last 2 MB is shown"})
    public String content;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        List<Map<String, String>> files = new ArrayList<>();
        for (Map<String, String> file : list()) {
            if (matches(file, query)) files.add(file);
        }
        List<String[]> rows = new ArrayList<>();
        for (Map<String, String> file : slice(files, pageNum, pageSize)) {
            rows.add(row(file, listFields));
        }
        return rows;
    }

    @Override
    public int total(Map<String, String> query) {
        int total = 0;
        for (Map<String, String> file : list()) {
            if (matches(file, query)) total++;
        }
        return total;
    }

    @Override
    public boolean contains(String id) {
        return NginxFiles.find(NginxFiles.listLogs(install()), id) != null;
    }

    @Override
    public Map<String, String> show(String id) {
        NginxInstall install = install();
        NginxFiles.Entry entry = NginxFiles.find(NginxFiles.listLogs(install), id);
        if (entry == null) throw new IllegalArgumentException("日志文件不存在: " + id);
        if (!install.isAllowed(entry.path, true)) {
            throw new IllegalStateException("日志文件不在允许访问的目录内: " + entry.path);
        }

        String text = NginxFiles.tailText(entry.path, NginxFiles.MAX_LOG_VIEW_BYTES);
        int lines = NginxFiles.lineCount(text);
        if (text.startsWith("...(仅显示最后")) lines = Math.max(0, lines - 1);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", entry.id);
        data.put("dir", entry.dir);
        data.put("size", NginxFiles.formatSize(entry.size));
        data.put("modified", entry.modified);
        data.put("type", logType(entry.path.getFileName().toString()));
        data.put("path", install.display(entry.path));
        data.put("lines", String.valueOf(lines));
        data.put("content", text);
        return data;
    }

    private List<Map<String, String>> list() {
        List<Map<String, String>> files = new ArrayList<>();
        try {
            for (NginxFiles.Entry entry : NginxFiles.listLogs(install())) {
                Map<String, String> file = new LinkedHashMap<>();
                file.put("name", entry.id);
                file.put("dir", entry.dir);
                file.put("size", NginxFiles.formatSize(entry.size));
                file.put("modified", entry.modified);
                file.put("type", logType(entry.path.getFileName().toString()));
                files.add(file);
            }
        } catch (NginxInstall.NotFoundException e) {
            // 未配置 nginx 时列表为空，概览页负责给出配置提示
        }
        return files;
    }

    private static String logType(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("error")) return "错误日志";
        if (lower.contains("access")) return "访问日志";
        return "其他";
    }

}
