package qingzhou.app.nginx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;

@Model(code = "config", order = 2, icon = "Document",
        name = {"配置管理", "en:Configuration"},
        info = {"查看和编辑 nginx 配置文件，保存时执行完整 nginx -t，失败回滚",
                "en:View and edit nginx configuration; full nginx -t runs on save with rollback"})
public class Config extends NginxModelBase implements Page, Show {

    @ModelField(id = true, list = true, search = true, readonly = true,
            name = {"文件", "en:File"},
            info = {"配置文件名；不同目录重名时附加路径哈希，避免 ID 冲突", "en:Config file name; hashed when duplicated"})
    public String name;

    @ModelField(list = true, search = true, readonly = true,
            name = {"目录", "en:Directory"},
            info = {"相对 nginx prefix 的目录", "en:Directory relative to the nginx prefix"})
    public String dir;

    @ModelField(list = true, readonly = true,
            name = {"大小", "en:Size"},
            info = {"文件大小", "en:File size"})
    public String size;

    @ModelField(list = true, readonly = true, input_type = InputType.datetime,
            name = {"修改时间", "en:Modified"},
            info = {"最后修改时间", "en:Last modified time"})
    public String modified;

    @ModelField(list = true, show = true, readonly = true, update = false,
            name = {"可编辑", "en:Editable"},
            info = {"true=可编辑，false=只读（约 1 MB 以上的文件不支持在线编辑）", "en:true when editable; files over about 1 MB are read-only"})
    public String editable;

    @ModelField(show = true, readonly = true,
            name = {"路径", "en:Path"},
            info = {"文件完整路径", "en:Full file path"})
    public String path;

    @ModelField(show = true, update = true, input_type = InputType.textarea,
            name = {"内容", "en:Content"},
            info = {"编辑上限约 1 MB；保存时替换目标文件并执行完整 nginx -t，失败自动回滚",
                    "en:Edit limit about 1 MB; full nginx -t runs on save with rollback"})
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
        return NginxFiles.find(NginxFiles.listConfigs(install()), id) != null;
    }

    @Override
    public Map<String, String> show(String id) {
        NginxInstall install = install();
        NginxFiles.Entry entry = NginxFiles.find(NginxFiles.listConfigs(install), id);
        if (entry == null) throw new IllegalArgumentException("配置文件不存在: " + id);
        if (!install.isAllowed(entry.path, false)) {
            throw new IllegalStateException("配置文件不在允许访问的目录内: " + entry.path);
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", entry.id);
        data.put("dir", entry.dir);
        boolean canEdit = entry.size <= NginxFiles.MAX_CONFIG_BYTES;
        data.put("size", NginxFiles.formatSize(entry.size));
        data.put("modified", entry.modified);
        data.put("path", install.display(entry.path));
        data.put("editable", String.valueOf(canEdit));
        data.put("content", canEdit
                ? NginxFiles.readText(entry.path, NginxFiles.MAX_CONFIG_BYTES)
                : "文件超过 " + NginxFiles.formatSize(NginxFiles.MAX_CONFIG_BYTES) + "，不支持在线查看和编辑。");
        return data;
    }

    @ModelAction(code = "update", icon = "Check", order = 1, update = true,
            name = {"保存", "en:Save"},
            info = {"保存前对完整主配置执行 nginx -t，校验失败自动回滚。",
                    "en:Full nginx -t runs before saving; invalid content is rolled back."})
    public void save(Request request) {
        execute(request, () -> {
            NginxInstall install = install();
            NginxFiles.Entry entry = NginxFiles.find(NginxFiles.listConfigs(install), request.getId());
            if (entry == null) throw new IllegalArgumentException("配置文件不存在: " + request.getId());

            String newContent = request.getParameter("content");
            if (newContent == null) throw new IllegalArgumentException("缺少配置内容");
            NginxFiles.updateConfig(new NginxService(install), entry, newContent, backupDir().toPath());
            request.getResponse().success(true).message("配置已保存，重载 nginx 后生效");
        });
    }

    @ModelAction(code = "test", icon = "CircleCheck", order = 1, list_head = true,
            name = {"校验配置", "en:Test Config"},
            info = {"执行 nginx -t 校验当前配置。",
                    "en:Run nginx -t against the current configuration."})
    public void test(Request request) {
        testConfig(request);
    }

    private List<Map<String, String>> list() {
        List<Map<String, String>> files = new ArrayList<>();
        try {
            for (NginxFiles.Entry entry : NginxFiles.listConfigs(install())) {
                Map<String, String> file = new LinkedHashMap<>();
                file.put("name", entry.id);
                file.put("dir", entry.dir);
                file.put("size", NginxFiles.formatSize(entry.size));
                file.put("modified", entry.modified);
                file.put("editable", String.valueOf(entry.size <= NginxFiles.MAX_CONFIG_BYTES));
                files.add(file);
            }
        } catch (NginxInstall.NotFoundException e) {
            // 未配置 nginx 时列表为空，概览页负责给出配置提示
        }
        return files;
    }

}
