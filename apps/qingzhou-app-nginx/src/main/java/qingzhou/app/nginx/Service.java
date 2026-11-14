package qingzhou.app.nginx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.Request;
import qingzhou.api.action.Page;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelField;

@Model(code = "service", order = 1, icon = "SetUp",
        name = {"服务控制", "en:Service Control"},
        info = {"启动、停止、重载与配置校验", "en:Start, stop, reload and config test"})
public class Service extends NginxModelBase implements Page {

    @ModelField(id = true, list = true, readonly = true,
            name = {"实例", "en:Instance"},
            info = {"nginx 实例标识", "en:nginx instance identifier"})
    public String id;

    @ModelField(list = true, readonly = true,
            name = {"状态", "en:Status"},
            info = {"running=运行中，stopped=已停止；not found=未定位到 nginx", "en:running, stopped, or not found"})
    public String status;

    @ModelField(list = true, readonly = true,
            name = {"主进程 PID", "en:Master PID"},
            info = {"来自 nginx pid 文件；操作前会校验该 PID 确为 nginx master", "en:Master PID from the nginx pid file"})
    public String pid;

    @ModelField(list = true, readonly = true,
            name = {"Worker 数量", "en:Workers"},
            info = {"按 master PID 的子进程关系统计 worker，采集失败时留空", "en:Worker processes counted by parent PID"})
    public String workers;

    @ModelField(list = true, readonly = true,
            name = {"版本", "en:Version"},
            info = {"nginx 版本号", "en:nginx version"})
    public String version;

    @ModelField(list = true, readonly = true,
            name = {"安装目录", "en:Install Path"},
            info = {"本实例实际使用的 nginx prefix（启动时的 -p 目录）", "en:nginx prefix used by this instance"})
    public String path;

    @ModelField(list = true, readonly = true,
            name = {"说明", "en:Message"},
            info = {"未找到 nginx 等异常时的提示", "en:Message when nginx is unavailable"})
    public String message;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        if (pageNum > 1) return new ArrayList<>();
        Map<String, String> snapshot = snapshot();
        List<String[]> rows = new ArrayList<>();
        if (matches(snapshot, query)) rows.add(row(snapshot, listFields));
        return rows;
    }

    @Override
    public int total(Map<String, String> query) {
        return matches(snapshot(), query) ? 1 : 0;
    }

    @Override
    public boolean contains(String id) {
        return "nginx".equals(id);
    }

    @ModelAction(code = "start", icon = "VideoPlay", order = 1, list = true,
            display = "status!=running",
            name = {"启动", "en:Start"},
            info = {"启动本机 nginx。", "en:Start nginx on this host."})
    public void start(Request request) {
        execute(request, () -> {
            NginxService service = service();
            service.start();
            request.getResponse().success(true)
                    .message("nginx 已启动，PID " + service.pidOrZero());
        });
    }

    @ModelAction(code = "stop", icon = "VideoPause", order = 2, list = true,
            display = "status==running",
            confirm = {"确认停止 nginx？", "en:Stop nginx?"},
            name = {"停止", "en:Stop"},
            info = {"优雅停止 nginx（quit）。", "en:Gracefully stop nginx (quit)."})
    public void stop(Request request) {
        execute(request, () -> {
            service().stop(false);
            request.getResponse().success(true).message("nginx 已停止");
        });
    }

    @ModelAction(code = "forceStop", icon = "SwitchButton", order = 3, list = true,
            display = "status==running",
            confirm = {"确认强制停止 nginx？未处理完的请求会被中断。",
                    "en:Force stop nginx? In-flight requests will be terminated."},
            name = {"强制停止", "en:Force Stop"},
            info = {"快速停止 nginx（stop）。", "en:Stop nginx immediately (stop)."})
    public void forceStop(Request request) {
        execute(request, () -> {
            service().stop(true);
            request.getResponse().success(true).message("nginx 已强制停止");
        });
    }

    @ModelAction(code = "reload", icon = "Refresh", order = 4, list = true,
            display = "status==running",
            name = {"重载配置", "en:Reload"},
            info = {"先执行 nginx -t，再发送 reload 信号；已有连接保持，是否生效以 error.log 为准。",
                    "en:Run nginx -t, then send reload; existing connections are kept."})
    public void reload(Request request) {
        execute(request, () -> {
            service().reload();
            request.getResponse().success(true)
                    .message("重载信号已发送，请查看 error.log 确认是否生效");
        });
    }

    @ModelAction(code = "restart", icon = "RefreshRight", order = 5, list = true,
            display = "status==running",
            confirm = {"确认重启 nginx？服务会短暂中断。",
                    "en:Restart nginx? The service will be briefly unavailable."},
            name = {"重启", "en:Restart"},
            info = {"先校验配置，再优雅停止并重新启动；服务会短暂中断。",
                    "en:Validate, gracefully stop, then start again."})
    public void restart(Request request) {
        execute(request, () -> {
            NginxService service = service();
            service.restart();
            request.getResponse().success(true)
                    .message("nginx 已重启，PID " + service.pidOrZero());
        });
    }

    @ModelAction(code = "test", icon = "CircleCheck", order = 6, list = true,
            name = {"校验配置", "en:Test Config"},
            info = {"执行 nginx -t 校验完整主配置，只检查不重载。",
                    "en:Run nginx -t against the full main configuration."})
    public void test(Request request) {
        testConfig(request);
    }

    private Map<String, String> snapshot() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", "nginx");
        try {
            NginxService service = service();
            data.put("status", service.statusText());
            data.put("version", service.version());
            data.put("path", NginxInstall.absolute(service.getInstall().prefix));

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

}
