package qingzhou.app.nginx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "dashboard", order = 0, icon = "HomeFilled",
        name = {"概览", "en:Overview"},
        info = {"nginx 安装信息与运行状态", "en:nginx installation and runtime status"})
public class Dashboard extends NginxModelBase implements Monitor {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ModelField(id = true, field_type = FieldType.monitor, show = false,
            name = {"编号", "en:ID"})
    public String id;

    @ModelField(field_type = FieldType.monitor, group = "Basic",
            name = {"统计时间", "en:Stats Time"},
            info = {"状态采集时间", "en:Status collection time"})
    public String statsTime;

    @ModelField(field_type = FieldType.monitor, group = "Basic",
            name = {"运行状态", "en:Status"},
            info = {"running: 运行中；stopped: 已停止；not found: 未找到 nginx",
                    "en:running, stopped, or not found"})
    public String status;

    @ModelField(field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge,
            group = "Basic",
            name = {"运行中", "en:Running"},
            info = {"1 表示运行中，0 表示未运行", "en:1 when running, 0 otherwise"})
    public String running;

    @ModelField(field_type = FieldType.monitor, group = "Basic",
            name = {"版本", "en:Version"},
            info = {"nginx 版本号", "en:nginx version"})
    public String version;

    @ModelField(field_type = FieldType.monitor, group = "Basic",
            name = {"主进程 PID", "en:Master PID"},
            info = {"nginx master 进程号", "en:nginx master process id"})
    public String pid;

    @ModelField(field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat,
            group = "Basic",
            name = {"Worker 数量", "en:Workers"},
            info = {"当前 worker 进程数", "en:Number of worker processes"})
    public String workers;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"安装目录", "en:Install Path"},
            info = {"nginx prefix 目录", "en:nginx prefix directory"})
    public String path;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"可执行文件", "en:Binary"},
            info = {"nginx 可执行文件路径", "en:nginx executable path"})
    public String binary;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"主配置", "en:Config File"},
            info = {"当前使用的主配置文件", "en:Main configuration file in use"})
    public String conf;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"PID 文件", "en:PID File"},
            info = {"nginx pid 文件路径", "en:nginx pid file path"})
    public String pidFile;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"日志目录", "en:Log Directories"},
            info = {"可查看的日志目录", "en:Log directories available for viewing"})
    public String logDirs;

    @ModelField(field_type = FieldType.monitor, group = "Config",
            name = {"编译参数", "en:Configure Arguments"},
            info = {"nginx 编译期参数，用于确认模块与默认路径",
                    "en:nginx configure arguments"})
    public String configureArguments;

    @ModelField(field_type = FieldType.monitor, group = "Basic",
            name = {"说明", "en:Message"},
            info = {"未找到 nginx 等异常时的提示", "en:Message when nginx is unavailable"})
    public String message;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", "nginx");
        data.put("statsTime", TIME_FORMAT.format(LocalDateTime.now()));
        data.put("running", "0");

        try {
            NginxInstall install = install();
            NginxService service = new NginxService(install);

            data.put("path", NginxInstall.absolute(install.prefix));
            data.put("binary", NginxInstall.absolute(install.binary));
            data.put("conf", NginxInstall.absolute(install.conf));
            data.put("pidFile", NginxInstall.absolute(install.pidFile));
            data.put("logDirs", install.displayLogDirs());
            data.put("version", emptyAsDash(service.version()));
            data.put("configureArguments", emptyAsDash(install.configureArguments));

            boolean isRunning = service.isRunning();
            data.put("status", isRunning ? "running" : "stopped");
            data.put("running", isRunning ? "1" : "0");

            int pid = service.pidOrZero();
            if (pid > 0) data.put("pid", String.valueOf(pid));
            int workers = service.workerCount();
            if (workers >= 0) data.put("workers", String.valueOf(workers));
        } catch (NginxInstall.NotFoundException e) {
            data.put("status", "not found");
            data.put("message", e.getMessage());
        }
        return data;
    }

    private static String emptyAsDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
