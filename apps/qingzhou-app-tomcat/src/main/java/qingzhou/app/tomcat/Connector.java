package qingzhou.app.tomcat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.action.*;

@Model(code = "connector", order = 4,
        name = {"Connector通道", "en:Connector"},
        info = {"展示Tomcat Connector配置信息", "en:Display Tomcat Connector configuration"},
        icon = "Fries")
public class Connector extends TomcatModelBase implements Page, Show {
    private java.util.List<Map<String, String>> db;

    public Connector() {
    }

    @ModelField(id = true,
            name = {"Connector ID", "en:Connector ID"},
            list = true,
            show = true,
            readonly = true)
    public String id;

    @ModelField(
            name = {"协议", "en:Protocol"},
            info = {"Connector使用的协议", "en:Protocol used by Connector"},
            list = true, search = true,
            show = true)
    public String protocol;

    @ModelField(
            name = {"端口", "en:Port"},
            info = {"Connector监听的端口", "en:Port number for Connector"},
            list = true, required = true, search = true, port = true, update = false,
            show = true)
    public String port;

    @ModelField(
            name = {"地址", "en:Address"},
            info = {"Connector绑定的地址", "en:Address bound by Connector"},
            list = true, search = true, host = true,
            show = true)
    public String address;

    @ModelField(
            name = {"最大线程数", "en:Max Threads"},
            info = {"最大工作线程数", "en:Maximum number of worker threads"},
            list = true, numeric = true,
            show = true)
    public String maxThreads;

    @ModelField(
            name = {"最小空闲线程", "en:Min Spare Threads"},
            info = {"最小空闲线程数", "en:Minimum number of spare threads"},
            list = true, numeric = true,
            show = true)
    public String minSpareThreads;

    @ModelField(
            name = {"等待队列", "en:Accept Count"},
            info = {"等待队列长度", "en:Length of the wait queue"},
            list = true, numeric = true,
            show = true)
    public String acceptCount;

    @ModelField(
            name = {"超时时间", "en:Timeout"},
            info = {"连接超时时间(毫秒)", "en:Connection timeout in milliseconds"},
            show = true, numeric = true)
    public String connectionTimeout;

    @ModelField(
            name = {"重定向端口", "en:Redirect Port"},
            info = {"SSL重定向端口", "en:SSL redirect port"},
            show = true, port = true)
    public String redirectPort;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"累计请求数", "en:Requests"},
            info = {"该通道自启动以来的累计请求数", "en:Requests since startup"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Request")
    public String requestCount;

    @ModelField(
            name = {"累计错误数", "en:Errors"},
            info = {"该通道自启动以来的累计错误数", "en:Errors since startup"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Request")
    public String errorCount;

    @ModelField(
            name = {"已发送(MB)", "en:Sent (MB)"},
            info = {"该通道累计发送字节数", "en:Bytes sent"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Traffic")
    public String bytesSent;

    @ModelField(
            name = {"已接收(MB)", "en:Received (MB)"},
            info = {"该通道累计接收字节数", "en:Bytes received"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Traffic")
    public String bytesReceived;

    @ModelField(
            name = {"线程繁忙率(%)", "en:Busy Threads (%)"},
            info = {"繁忙线程数占最大线程数的比例", "en:Busy threads ratio"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "Thread")
    public String threadBusyPercent;

    @ModelField(
            name = {"繁忙线程", "en:Busy Threads"},
            info = {"当前处理请求的线程数", "en:Threads processing requests"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "Thread")
    public String threadBusy;

    @ModelField(
            name = {"空闲线程", "en:Idle Threads"},
            info = {"当前可用的空闲线程数", "en:Idle threads"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "Thread")
    public String threadIdle;

    @ModelField(
            name = {"平均处理时间(ms)", "en:Avg Time (ms)"},
            info = {"该通道请求平均处理耗时", "en:Average processing time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Latency")
    public String avgProcessingTime;

    @ModelField(
            name = {"最大处理时间(ms)", "en:Max Time (ms)"},
            info = {"该通道请求历史最大处理耗时", "en:Maximum processing time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Latency")
    public String maxProcessingTime;

    @Override
    public java.util.List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        db = loadConnectors();
        List<Map<String, String>> filtered = filterByQuery(db, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return db.size();

    }

    @Override
    public boolean contains(String id) {
        for (Map<String, String> connector : db) {
            if (connector.get("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, String> show(String id) {
        if (id == null) {
            return new HashMap<>();
        }
        for (Map<String, String> connector : db) {
            if (connector.get("id").equals(id)) {
                return connector;
            }
        }
        return new HashMap<>();
    }

    // 覆盖框架默认 monitor 动作：默认动作未声明 list，列表行不会生成"监视"按钮
    @ModelAction(code = Monitor.ACTION_CODE_MONITOR, icon = "TrendCharts", list = true,
            name = {"监视", "en:Monitor"},
            info = {"查看该通道的实时运行指标。", "en:View realtime metrics of this connector."})
    public void monitor(Request request) throws Exception {
        request.getResponse().data(TomcatJmx.read(getTomcatBaseDir(), jmx -> jmx.connectorStats(monitorId(request))));
    }

    /**
     * 只读运行中 Tomcat 的 Connector MBean，Tomcat 未运行或无法 attach 时直接报错。
     */
    private java.util.List<Map<String, String>> loadConnectors() {
        try {
            return TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::connectors);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read Tomcat connectors via JMX", e);
        }
    }
}
