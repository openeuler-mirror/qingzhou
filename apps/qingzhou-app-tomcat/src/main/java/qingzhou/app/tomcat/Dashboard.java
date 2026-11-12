package qingzhou.app.tomcat;

import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "dashboard",
        order = 0,
        name = {"首页", "en:Dashboard"},
        info = {"Tomcat 运行监控", "en:Tomcat runtime monitoring"},
        icon = "HomeFilled")
public class Dashboard extends TomcatModelBase implements Show, Monitor {

    @ModelField(id = true, show = false,
            name = {"编号", "en:ID"},
            field_type = FieldType.monitor)
    public String id;

    @ModelField(
            name = {"运行状态", "en:Status"},
            info = {"Tomcat 进程运行状态", "en:Tomcat process running status"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String running;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"安装路径", "en:Install Path"},
            info = {"Tomcat 安装路径", "en:Tomcat installation path"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String path;

    @ModelField(
            name = {"服务器版本", "en:Server Info"},
            info = {"Tomcat 版本信息", "en:Tomcat version"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String serverInfo;

    @ModelField(
            name = {"通道数", "en:Connectors"},
            info = {"当前 Connector 数量", "en:Number of connectors"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String connectorCount;

    @ModelField(
            name = {"应用数", "en:Applications"},
            info = {"当前部署的应用数量", "en:Number of deployed applications"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String applicationCount;

    @Override
    public Map<String, String> show(String id) {
        try {
            Map<String, String> stats = TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::stats);
            stats.put("path", getAppContext().getDetectedPath());
            stats.put("running", "运行");
            return stats;
        } catch (TomcatNotRunningException e) {
            Map<String, String> offline = new java.util.LinkedHashMap<>();
            offline.put("path", getAppContext().getDetectedPath());
            offline.put("running", "未启动");
            return offline;
        } catch (Exception e) {
            throw new IllegalStateException("failed to read Tomcat metrics via JMX", e);
        }
    }

    @Override
    public Map<String, String> monitor(String id) {
        return show(id);
    }
}
