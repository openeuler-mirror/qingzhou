package qingzhou.app.system.business;

import qingzhou.api.*;
import qingzhou.api.type.Download;
import qingzhou.api.type.Group;
import qingzhou.api.type.List;
import qingzhou.api.type.Monitor;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.InstanceInfo;
import qingzhou.core.registry.Registry;
import qingzhou.engine.ModuleContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Model(code = DeployerConstants.MODEL_INSTANCE, icon = "stack",
        menu = Main.Business, order = 2,
        name = {"实例", "en:Instance"},
        info = {"实例是应用部署的载体，为应用提供运行时环境。预置的 " + DeployerConstants.INSTANCE_LOCAL + " 实例表示当前正在访问的服务所在的实例，如集中管理端就运行在此实例上。",
                "en:An instance is the carrier of application deployment and provides a runtime environment for the application. The provisioned " + DeployerConstants.INSTANCE_LOCAL + " instance indicates the instance where the service is currently accessed, such as the centralized management side running on this instance."})
public class Instance extends ModelBase implements List, Monitor, Group, Download {
    private static final String ID_KEY = "name";

    @ModelField(
            required = true,
            search = true,
            name = {"实例名称", "en:Instance Name"},
            info = {"表示该实例的名称，用于识别和管理该实例。",
                    "en:Indicates the name of the instance, which is used to identify and manage the instance."})
    public String name;

    @ModelField(
            list = true, search = true,
            name = {"主机IP", "en:Host IP"},
            info = {"该实例所在服务器的域名或 IP 地址。",
                    "en:The domain name or IP address of the server where the instance resides."})
    public String host;

    @ModelField(
            list = true, search = true,
            name = {"管理端口", "en:Management Port"},
            info = {"该实例所开放的管理端口，用以受理轻舟集中管理端发来的业务请求。",
                    "en:The management port opened by the instance is used to accept business requests from the centralized management end of Qingzhou."})
    public Integer port;

    @ModelField(
            list = true, search = true,
            name = {"平台版本", "en:Version"},
            info = {"该实例运行的轻舟版本。", "en:The Qingzhou version that the instance runs."})
    public String version;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR,
            name = {"OS", "en:OS"}, info = {"操作系统的名称。", "en:The name of the operating system."})
    public String osName;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR,
            name = {"版本", "en:OS Version"}, info = {"操作系统的版本。", "en:The version of the operating system."})
    public String osVer;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR,
            name = {"架构", "en:OS Architecture"}, info = {"服务器的架构。", "en:The architecture of the server."})
    public String arch;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR,
            name = {"CPU 个数", "en:Number Of Cpus"}, info = {"服务器的处理器数量。", "en:The number of processors on the server."})
    public int cpu;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR,
            name = {"硬盘（GB）", "en:Disk (GB)"},
            info = {"硬盘总空间大小，单位GB。", "en:The total size of the hard disk, in GB."})
    public double disk;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"系统负载", "en:Load Average"},
            info = {"表示当前系统 CPU 的总体利用率，范围在 0.0 ~ 1.0 之间。", "en:Indicates the overall utilization of the current system CPU, ranging from 0.0 ~ 1.0."})
    public double cpuUsed;

    @ModelField(
            group = group_os,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"使用硬盘（GB）", "en:Disk Used (GB)"},
            info = {"当前已使用的硬盘空间，单位GB。", "en:The disk currently used, in GB."})
    public double diskUsed;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Java 规格", "en:Java Spec"},
            info = {"Java 虚拟机规范名称。", "en:Java virtual machine specification name."})
    public String specName;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Java 版本", "en:Java Version"},
            info = {"Java 虚拟机规范版本。", "en:Java virtual machine specification version."})
    public String specVersion;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Jvm 名称", "en:Jvm Name"},
            info = {"Java 虚拟机名称。", "en:The Java virtual machine implementation name."})
    public String vmName;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Jvm 供应商", "en:Jvm Version"},
            info = {"Java 虚拟机供应商。", "en:JVM software vendor."})
    public String vmVendor;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Jvm 版本", "en:Jvm Vendor"},
            info = {"Java 虚拟机版本。", "en:JVM software version."})
    public String vmVersion;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"Jvm 进程", "en:Jvm Process"},
            info = {"当前运行服务的操作系统PID。", "en:PID of the operating system currently running the service."})
    public String Name;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"启动时间", "en:Start Time"},
            info = {"Java 虚拟机的启动时间。", "en:The Java virtual machine startup time."})
    public String startTime;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR,
            name = {"最大堆内存（MB）", "en:Heap Memory Max (MB)"},
            info = {"可使用的最大堆内存，单位MB。", "en:The maximum heap memory that can be used, in MB."})
    public Double heapCommitted;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"Jvm 线程总数", "en:Jvm Thread Count"},
            info = {"当前活动线程的数量，包括守护线程和非守护线程。", "en:The current number of live threads including both daemon and non-daemon threads."})
    public int threadCount;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"死锁线程数", "en:Deadlocked Threads"},
            info = {"死锁等待对象监视器或同步器的线程数。", "en:The number of threads deadlocked waiting for an object monitor or synchronizer."})
    public int deadlockedThreadCount;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"使用中堆内存（MB）", "en:Heap Memory Used (MB)"},
            info = {"正在使用的堆内存的大小，单位MB。", "en:The size of the heap memory in use, in MB."})
    public Double heapUsed;

    @ModelField(
            group = group_jvm,
            field_type = FieldType.MONITOR, numeric = true,
            name = {"使用中非堆内存（MB）", "en:Non-Heap Memory Used (MB)"},
            info = {"正在使用的非堆内存的大小，单位MB。", "en:The size of the non-heap memory in use, in MB."})
    public Double nonHeapUsed;

    public final String group_os = "os";
    private final String group_jvm = "jvm";

    @Override
    public Item[] groupData() {
        return new Item[]{
                Item.of(group_os, new String[]{"操作系统", "en:Operating System"}),
                Item.of(group_jvm, new String[]{"Java 虚拟机", "en:Java VM"})
        };
    }

    @Override
    public String idField() {
        return ID_KEY;
    }

    @Override
    public String[] allIds(Map<String, String> query) {
        return allInstanceIds(query);
    }

    public static String[] allInstanceIds(Map<String, String> query) {
        java.util.List<String> ids = new ArrayList<>();
        ids.add(DeployerConstants.INSTANCE_LOCAL);
        Registry registry = Main.getService(Registry.class);
        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            ids.add(instanceInfo.getName());
        });
        ids.removeIf(id -> !ModelUtil.query(query, new ModelUtil.Supplier() {
            @Override
            public String getModelName() {
                return DeployerConstants.MODEL_INSTANCE;
            }

            @Override
            public Map<String, String> get() {
                return showData(id);
            }
        }));
        return ids.toArray(new String[0]);
    }

    @Override
    public java.util.List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws IOException {
        return ModelUtil.listData(allIds(query), Instance::showData, pageNum, pageSize, showFields);
    }

    private static Map<String, String> showData(String id) {
        if (DeployerConstants.INSTANCE_LOCAL.equals(id)) {
            return new HashMap<String, String>() {{
                put(ID_KEY, DeployerConstants.INSTANCE_LOCAL);
                put("host", "--");
                put("port", "--");
                put("version", Main.getService(ModuleContext.class).getPlatformVersion());
            }};
        }

        InstanceInfo instanceInfo = Main.getService(Registry.class).getInstanceInfo(id);
        if (instanceInfo != null) {
            return new HashMap<String, String>() {{
                put(ID_KEY, instanceInfo.getName());
                put("host", instanceInfo.getHost());
                put("port", String.valueOf(instanceInfo.getPort()));
                put("version", instanceInfo.getVersion());
            }};
        }

        return null;
    }

    @ModelAction(
            code = Download.ACTION_FILES, icon = "download-alt",
            list_action = true,
            action_type = ActionType.download,
            name = {"下载日志", "en:Download Log"},
            info = {"下载实例的日志信息。", "en:Download the log information of the instance."})
    public void files(Request request) {
        invokeOnAgent(request, request.getId());
    }

    @ModelAction(
            code = Download.ACTION_DOWNLOAD, icon = "download-alt",
            name = {"下载日志", "en:Download Log"},
            info = {"下载实例的日志信息。", "en:Download the log information of the instance."})
    public void download(Request request) {
        invokeOnAgent(request, request.getId());
    }

    @ModelAction(
            code = Monitor.ACTION_MONITOR, icon = "line-chart",
            list_action = true,
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request) throws Exception {
        invokeOnAgent(request, request.getId());
        ResponseImpl response = (ResponseImpl) request.getResponse();
        tempData.set((Map<String, String>) response.getInternalData()); // dataList 不应为空，来自：qingzhou.app.system.Agent.monitor（xxx）
        getAppContext().invokeSuperAction(request); // 触发调用下面的 monitorData（使用 tempData）；
        tempData.remove();
    }

    // 为了复用 SuperAction 的 monitor 方法逻辑
    private final ThreadLocal<Map<String, String>> tempData = new ThreadLocal<>();

    @Override
    public Map<String, String> monitorData(String id) {
        return tempData.get();
    }

    private void invokeOnAgent(Request request, String instance) {
        String originModel = request.getModel();
        RequestImpl requestImpl = (RequestImpl) request;
        try {
            requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
            Map<String, Response> invokeOnInstances = Main.getService(ActionInvoker.class)
                    .invokeOnInstances(request, instance);
            requestImpl.setResponse(invokeOnInstances.values().iterator().next());
        } finally {
            requestImpl.setModelName(originModel);
        }
    }

    @Override
    public File downloadData(String id) {
        return null;//已经自行实现了下载方法，不必在覆写这个
    }
}
