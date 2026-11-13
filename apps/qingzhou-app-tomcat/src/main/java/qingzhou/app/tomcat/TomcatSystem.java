package qingzhou.app.tomcat;

import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "system", order = 2, icon = "Monitor",
        name = {"系统监控", "en:System Monitor"},
        info = {"Tomcat 进程与操作系统资源指标", "en:Process and operating system metrics of Tomcat"})
public class TomcatSystem extends TomcatModelBase implements Monitor {

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"监控数据采集时间", "en:Statistics generation time"}, field_type = FieldType.monitor, group = "Process")
    public String statsTime;
    @ModelField(name = {"进程PID", "en:Process PID"}, info = {"运行中 Tomcat 的进程号", "en:Process id of the running Tomcat"}, field_type = FieldType.monitor, group = "Process")
    public String pid;
    @ModelField(name = {"进程启动时间", "en:Start Time"}, info = {"Tomcat 进程启动时间", "en:Process start time"}, field_type = FieldType.monitor, group = "Process")
    public String startTime;
    @ModelField(name = {"JVM版本", "en:JVM Version"}, info = {"Tomcat 进程使用的 JVM", "en:JVM of the Tomcat process"}, field_type = FieldType.monitor, group = "Process")
    public String jvmName;
    @ModelField(name = {"物理内存(MB)", "en:RSS (MB)"}, info = {"进程常驻物理内存", "en:Resident physical memory"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Process")
    public String rssMb;
    @ModelField(name = {"物理内存占比(%)", "en:RSS (%)"}, info = {"进程常驻内存占整机物理内存的比例", "en:Resident memory ratio"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge, group = "Process")
    public String rssPercent;
    @ModelField(name = {"启动参数", "en:VM Arguments"}, info = {"Tomcat 进程的 JVM 启动参数", "en:JVM arguments of the Tomcat process"}, field_type = FieldType.monitor, group = "Process")
    public String vmArgs;
    @ModelField(name = {"系统CPU(%)", "en:System CPU (%)"}, info = {"整机 CPU 使用率", "en:System-wide CPU usage"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge, group = "Os")
    public String systemCpuPercent;
    @ModelField(name = {"物理内存总量(MB)", "en:Total Physical (MB)"}, info = {"整机物理内存总量", "en:Total physical memory"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Os")
    public String physicalTotalMb;
    @ModelField(name = {"物理内存可用(MB)", "en:Free Physical (MB)"}, info = {"操作系统可用物理内存", "en:Free physical memory"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Os")
    public String physicalFreeMb;
    @ModelField(name = {"打开文件数", "en:Open Files"}, info = {"进程当前打开的文件句柄数", "en:Open file descriptors"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "File")
    public String openFileCount;
    @ModelField(name = {"文件句柄上限", "en:Max Files"}, info = {"进程文件句柄上限", "en:File descriptor limit"}, field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "File")
    public String maxFileCount;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        return TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::systemStats);
    }
}
