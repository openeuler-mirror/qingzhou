package qingzhou.app.tomcat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;

@Model(code = "threadpool", order = 5, icon = "Coin",
        name = {"线程池", "en:Thread Pools"},
        info = {"Tomcat线程池监控", "en:Tomcat thread pool monitoring"})
public class TomcatThreadPool extends TomcatModelBase implements Page, Show {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"池名称", "en:Pool Name"},
            info = {"线程池名称", "en:Thread pool name"})
    public String name;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"端口", "en:Port"},
            info = {"线程池所属 Connector 的端口", "en:Port of the owning connector"})
    public String port;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"本地端口", "en:Local Port"},
            info = {"实际监听端口", "en:Actual listening port"})
    public String localPort;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"最大线程", "en:Max Threads"},
            info = {"配置的最大线程数", "en:Maximum configured threads"})
    public String maxThreads;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"最小空闲", "en:Min Spare"},
            info = {"最小空闲线程数", "en:Minimum spare threads"})
    public String minSpareThreads;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"当前线程数", "en:Current Threads"},
            info = {"当前已创建的线程数", "en:Current number of threads"})
    public String currentThreadCount;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"繁忙线程数", "en:Busy Threads"},
            info = {"当前正在处理请求的线程数", "en:Threads currently processing requests"})
    public String currentThreadsBusy;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"连接数", "en:Connections"},
            info = {"当前打开的连接数", "en:Current number of open connections"})
    public String connectionCount;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"长连接数", "en:Keep-Alive"},
            info = {"当前保持的长连接数", "en:Current number of keep-alive connections"})
    public String keepAliveCount;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"最大连接数", "en:Max Connections"},
            info = {"允许的最大连接数", "en:Maximum number of connections"})
    public String maxConnections;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"等待队列", "en:Backlog"},
            info = {"等待接受的连接队列长度", "en:Accept backlog"})
    public String backlog;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"接受线程数", "en:Acceptor Threads"},
            info = {"Acceptor 线程数", "en:Number of acceptor threads"})
    public String acceptorThreadCount;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"轮询线程数", "en:Poller Threads"},
            info = {"Poller 线程数", "en:Number of poller threads"})
    public String pollerThreadCount;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"长连接超时", "en:Keep-Alive Timeout"},
            info = {"长连接超时时间(毫秒)", "en:Keep-alive timeout in milliseconds"})
    public String keepAliveTimeout;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"最大长连接请求", "en:Max Keep-Alive Requests"},
            info = {"单个长连接允许的最大请求数", "en:Max requests per keep-alive connection"})
    public String maxKeepAliveRequests;

    @ModelField(show = true, readonly = true, numeric = true,
            name = {"读取超时", "en:Socket Timeout"},
            info = {"Socket 读取超时时间(毫秒)", "en:Socket read timeout in milliseconds"})
    public String soTimeout;

    @ModelField(show = true, readonly = true,
            name = {"运行状态", "en:Running"},
            info = {"线程池是否运行中", "en:Whether the thread pool is running"})
    public String running;

    @ModelField(show = true, readonly = true,
            name = {"暂停状态", "en:Paused"},
            info = {"线程池是否已暂停", "en:Whether the thread pool is paused"})
    public String paused;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"已创建线程", "en:Created Threads"},
            info = {"线程池已创建的 worker 线程数", "en:Worker threads created"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Thread")
    public String threadCount;

    @ModelField(
            name = {"繁忙线程", "en:Busy Threads"},
            info = {"当前处理请求的线程数", "en:Threads processing requests"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.bar, group = "Thread")
    public String busyThreads;

    @ModelField(
            name = {"空闲线程", "en:Idle Threads"},
            info = {"已创建但未处理请求的线程数", "en:Created threads not processing requests"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.bar, group = "Thread")
    public String idleThreads;

    @ModelField(
            name = {"线程繁忙率(%)", "en:Busy Threads (%)"},
            info = {"繁忙线程占最大线程数的比例", "en:Busy threads ratio"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.gauge, group = "Thread")
    public String busyPercent;

    @ModelField(
            name = {"当前连接数", "en:Live Connections"},
            info = {"当前打开的连接数", "en:Open connections"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Connection")
    public String liveConnections;

    @ModelField(
            name = {"长连接数", "en:Keep-Alive Connections"},
            info = {"当前保持的长连接数", "en:Keep-alive connections"},
            field_type = FieldType.monitor, numeric = true,
            chart_type = ChartType.stat, group = "Connection")
    public String keepAliveConnections;

    private List<Map<String, String>> threadPools;

    @ModelAction(code = Monitor.ACTION_CODE_MONITOR, icon = "TrendCharts", list = true,
            name = {"监视", "en:Monitor"},
            info = {"查看该线程池的实时运行指标。", "en:View realtime metrics of this thread pool."})
    public void monitor(Request request) throws Exception {
        request.getResponse().data(TomcatJmx.read(getTomcatBaseDir(), jmx -> jmx.threadPoolStats(monitorId(request))));
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) {
        loadThreadPools();
        List<Map<String, String>> filtered = filterByQuery(threadPools, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        loadThreadPools();
        return filterByQuery(threadPools, query).size();
    }

    @Override
    public boolean contains(String id) {
        loadThreadPools();
        return threadPools.stream().anyMatch(p -> p.get("name").equals(id));
    }

    @Override
    public Map<String, String> show(String id) {
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        loadThreadPools();

        String finalId = id;
        return threadPools.stream()
                .filter(p -> p.get("name").equals(finalId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 只读运行中 Tomcat 的 ThreadPool MBean，Tomcat 未运行或无法 attach 时直接报错。
     */
    private void loadThreadPools() {
        try {
            threadPools = TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::threadPools);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read Tomcat thread pools via JMX", e);
        }
    }
}