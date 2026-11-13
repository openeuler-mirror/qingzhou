package qingzhou.app.tomcat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.action.Page;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.*;

@Model(code = "threadpool", order = 5, icon = "Coin",
        name = {"线程池", "en:Thread Pools"}, info = {"请求处理线程的容量与负载监控", "en:Capacity and load of request processing threads"})
public class TomcatThreadPool extends TomcatListModel implements Page, Show {

    @ModelField(id = true, list = true, show = true, readonly = true, name = {"池名称", "en:Pool Name"}, info = {"线程池名称", "en:Thread pool name"})
    public String name;
    @ModelField(list = true, show = true, readonly = true, numeric = true, name = {"端口", "en:Port"}, info = {"线程池所属 Connector 的端口", "en:Port of the owning connector"})
    public String port;
    @ModelField(list = true, show = true, readonly = true, numeric = true, name = {"最大线程", "en:Max Threads"}, info = {"配置的最大线程数", "en:Maximum configured threads"})
    public String maxThreads;
    @ModelField(list = true, show = true, readonly = true, numeric = true, name = {"最小空闲", "en:Min Spare"}, info = {"最小空闲线程数", "en:Minimum spare threads"})
    public String minSpareThreads;
    @ModelField(list = true, show = true, readonly = true, numeric = true, name = {"当前线程数", "en:Current Threads"}, info = {"当前已创建的线程数", "en:Current number of threads"})
    public String currentThreadCount;
    @ModelField(list = true, show = true, readonly = true, numeric = true, name = {"繁忙线程数", "en:Busy Threads"}, info = {"当前正在处理请求的线程数", "en:Threads currently processing requests"})
    public String currentThreadsBusy;
    @ModelField(show = true, readonly = true, name = {"运行状态", "en:Running"}, info = {"线程池是否运行中", "en:Whether the thread pool is running"})
    public String running;
    @ModelField(show = true, readonly = true, name = {"暂停状态", "en:Paused"}, info = {"线程池是否已暂停", "en:Whether the thread pool is paused"})
    public String paused;
    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"监控数据采集时间", "en:Statistics generation time"}, field_type = FieldType.monitor, group = "Basic")
    public String statsTime;
    @ModelField(name = {"已创建线程", "en:Created Threads"}, info = {"线程池已创建的 worker 线程数", "en:Worker threads created"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat, group = "Thread")
    public String threadCount;
    @ModelField(name = {"繁忙线程", "en:Busy Threads"}, info = {"当前处理请求的线程数", "en:Threads processing requests"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "Thread")
    public String busyThreads;
    @ModelField(name = {"空闲线程", "en:Idle Threads"}, info = {"已创建但未处理请求的线程数", "en:Created threads not processing requests"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "Thread")
    public String idleThreads;
    @ModelField(name = {"线程繁忙率(%)", "en:Busy Threads (%)"}, info = {"繁忙线程占最大线程数的比例", "en:Busy threads ratio"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge, group = "Thread")
    public String busyPercent;

    @ModelAction(code = "monitor", icon = "TrendCharts", list = true,
            name = {"监视", "en:Monitor"}, info = {"查看该线程池的实时运行指标。", "en:View realtime metrics of this thread pool."})
    public void monitor(Request request) throws Exception {
        request.getResponse().data(readJmx(jmx -> jmx.threadPoolStats(monitorId(request))));
    }

    @Override
    protected List<Map<String, String>> loadData() throws Exception {
        return readJmx(TomcatJmx::threadPools);
    }

    @Override
    protected String idKey() {
        return "name";
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return findById(id);
    }
}
