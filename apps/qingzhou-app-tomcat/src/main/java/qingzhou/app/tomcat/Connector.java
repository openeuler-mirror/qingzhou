package qingzhou.app.tomcat;

import java.util.*;

import qingzhou.api.*;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.*;

@Model(code = "connector", order = 4,
        name = {"Connector通道", "en:Connector"},
        info = {"连接监听配置与请求流量监控", "en:Connection config and request traffic monitoring"},
        icon = "Fries")
public class Connector extends TomcatModelBase implements Page, Show {
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
            name = {"当前连接数", "en:Live Connections"},
            info = {"该通道当前打开的连接数", "en:Open connections of this connector"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Connection")
    public String liveConnections;

    @ModelField(
            name = {"长连接数", "en:Keep-Alive Connections"},
            info = {"该通道当前保持的长连接数", "en:Keep-alive connections of this connector"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Connection")
    public String keepAliveConnections;

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
        List<Map<String, String>> connectors = loadConnectors();
        return buildListResult(filterByQuery(connectors, query), pageNum, pageSize, listFields);
    }

    @Override
    public int total(Map<String, String> query) {
        return filterByQuery(loadConnectors(), query).size();
    }

    @Override
    public boolean contains(String id) {
        return loadConnectors().stream().anyMatch(connector -> connector.get("id").equals(id));
    }

    @Override
    public Map<String, String> show(String id) {
        return loadConnectors().stream()
                .filter(connector -> connector.get("id").equals(id))
                .findFirst()
                .orElse(new HashMap<>());
    }

    // 覆盖框架默认 monitor 动作：默认动作未声明 list，列表行不会生成"监视"按钮
    @ModelAction(code = "monitor", icon = "TrendCharts", list = true,
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
