package qingzhou.app.tomcat;

import qingzhou.api.*;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Page;

import java.util.*;

@Model(code = "applications", order = 1, icon = "ShoppingBag",
        name = {"应用管理", "en:Applications"},
        info = {"展示运行中 Tomcat 的应用(Context)", "en:Display applications of the running Tomcat"})
public class TomcatApplications extends TomcatModelBase implements Page {
    @ModelField(
            id = true,
            name = {"应用名称", "en:App Name"},
            info = {"应用名称，如 demo", "en:App name, e.g. demo"},
            list = true, add = false, update = false, show = true, readonly = true)
    public String name;

    @ModelField(
            name = {"上下文路径", "en:Context Path"},
            info = {"应用访问路径，如 /demo", "en:Application access path, e.g. /demo"},
            list = true, show = true, required = true)
    public String contextPath;

    @ModelField(
            name = {"文档基目录", "en:DocBase"},
            info = {"应用文档基目录", "en:Application document base directory"},
            list = true,
            add = true, update = true, required = true)
    public String docBase;

    @ModelField(
            name = {"可重载", "en:Reloadable"},
            info = {"是否自动重载", "en:Auto reload enabled"},
            list = true,
            input_type = InputType.select,
            options = {"true", "false"},
            add = true, update = true)
    public String reloadable;

    @ModelField(
            name = {"应用状态", "en:Status"},
            info = {"应用运行状态", "en:Application status"},
            list = true,
            show = true,
            readonly = true)
    public String status;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"累计请求数", "en:Requests"},
            info = {"该应用自启动以来的累计请求数", "en:Requests since startup"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Request")
    public String requestCount;

    @ModelField(
            name = {"累计错误数", "en:Errors"},
            info = {"该应用自启动以来的累计错误数", "en:Errors since startup"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Request")
    public String errorCount;

    @ModelField(
            name = {"平均处理时间(ms)", "en:Avg Time (ms)"},
            info = {"该应用请求平均处理耗时", "en:Average processing time"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.line, group = "Latency")
    public String avgProcessingTime;

    @ModelField(
            name = {"最大处理时间(ms)", "en:Max Time (ms)"},
            info = {"该应用请求历史最大处理耗时", "en:Maximum processing time"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.line, group = "Latency")
    public String maxProcessingTime;

    @ModelField(
            name = {"最小处理时间(ms)", "en:Min Time (ms)"},
            info = {"该应用请求历史最小处理耗时", "en:Minimum processing time"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Latency")
    public String minProcessingTime;

    @ModelField(
            name = {"活跃会话数", "en:Active Sessions"},
            info = {"该应用当前的活跃会话数", "en:Active sessions"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Session")
    public String activeSessions;

    @ModelField(
            name = {"运行状态", "en:State"},
            info = {"该应用的运行时状态", "en:Application runtime state"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String stateName;

    private List<Map<String, String>> applications;

    @ModelAction(code = Monitor.ACTION_CODE_MONITOR, icon = "TrendCharts", list = true,
            name = {"监视", "en:Monitor"},
            info = {"查看该应用的实时运行指标。", "en:View realtime metrics of this application."})
    public void monitor(Request request) throws Exception {
        request.getResponse().data(TomcatJmx.read(getTomcatBaseDir(), jmx -> jmx.applicationStats(monitorId(request))));
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) throws Exception {
        loadApplications();
        List<Map<String, String>> filtered = filterByQuery(applications, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        loadApplications();
        return filterByQuery(applications, query).size();
    }

    @Override
    public boolean contains(String id) {
        loadApplications();
        return applications.stream().anyMatch(a -> a.get("name").equals(id));
    }

    /**
     * 只读运行中 Tomcat 的 WebModule MBean，Tomcat 未运行或无法 attach 时直接报错。
     */
    private void loadApplications() {
        try {
            applications = TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::applications);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read Tomcat applications via JMX", e);
        }
    }
}
