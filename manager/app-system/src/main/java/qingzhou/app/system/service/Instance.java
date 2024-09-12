package qingzhou.app.system.service;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Downloadable;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Monitorable;
import qingzhou.app.system.Main;
import qingzhou.app.system.ModelUtil;
import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.ModuleContext;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = "instance", icon = "stack",
        menu = Main.SERVICE_MENU, order = 2,
        name = {"实例", "en:Instance"},
        info = {"实例是应用部署的载体，为应用提供运行时环境。预置的 " + DeployerConstants.INSTANCE_LOCAL + " 实例表示当前正在访问的服务所在的实例，如集中管理端就运行在此实例上。",
                "en:An instance is the carrier of application deployment and provides a runtime environment for the application. The provisioned " + DeployerConstants.INSTANCE_LOCAL + " instance indicates the instance where the service is currently accessed, such as the centralized management side running on this instance."})
public class Instance extends ModelBase implements Listable, Monitorable, Downloadable {
    @ModelField(
            required = true,
            list = true,
            name = {"实例名称", "en:Name"},
            info = {"表示该实例的名称，用于识别和管理该实例。",
                    "en:Indicates the name of the instance, which is used to identify and manage the instance."})
    public String name;

    @ModelField(
            required = true,
            host = true,
            list = true,
            name = {"主机IP", "en:Host IP"},
            info = {"该实例所在服务器的域名或 IP 地址。",
                    "en:The domain name or IP address of the server where the instance resides."})
    public String host;

    @ModelField(
            required = true,
            port = true,
            list = true,
            name = {"管理端口", "en:Management Port"},
            info = {"该实例所开放的管理端口，用以受理轻舟集中管理端发来的业务请求。",
                    "en:The management port opened by the instance is used to accept business requests from the centralized management end of Qingzhou."})
    public Integer port;

    @ModelField(
            monitor = true,
            list = true,
            name = {"运行中", "en:Running"},
            info = {"用以表示该实例是否正在运行。",
                    "en:This indicates whether the instance is running."})
    public Boolean running;

    @ModelField(monitor = true,
            name = {"OS", "en:OS"}, info = {"操作系统的名称。", "en:The name of the operating system."})
    public String osName;

    @ModelField(monitor = true,
            name = {"版本", "en:OS Version"}, info = {"操作系统的版本。", "en:The version of the operating system."})
    private String osVer;

    @ModelField(monitor = true,
            name = {"架构", "en:OS Architecture"}, info = {"服务器的架构。", "en:The architecture of the server."})
    private String arch;

    @ModelField(monitor = true,
            name = {"CPU 个数", "en:Number Of Cpus"}, info = {"服务器的处理器数量。", "en:The number of processors on the server."})
    public int cpu;

    @ModelField(monitor = true,
            name = {"硬盘（GB）", "en:Disk (GB)"},
            info = {"硬盘总空间大小，单位GB。", "en:The total size of the hard disk, in GB."})
    public double disk;

    @ModelField(monitor = true, numeric = true,
            name = {"系统负载", "en:Load Average"},
            info = {"表示当前系统 CPU 的总体利用率，范围在 0.0 ~ 1.0 之间。", "en:Indicates the overall utilization of the current system CPU, ranging from 0.0 ~ 1.0."})
    public double cpuUsed;

    @ModelField(monitor = true, numeric = true,
            name = {"使用硬盘（GB）", "en:Disk Used (GB)"},
            info = {"当前已使用的硬盘空间，单位GB。", "en:The disk currently used, in GB."})
    public double diskUsed;

    @Override
    public String idFieldName() {
        return "name";
    }

    @Override
    public String[] allIds() {
        List<String> ids = new ArrayList<>();
        ids.add(DeployerConstants.INSTANCE_LOCAL);
        Registry registry = Main.getService(Registry.class);
        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            ids.add(instanceInfo.getName());
        });
        return ids.toArray(new String[0]);
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(localInstance());

        Registry registry = Main.getService(Registry.class);
        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            result.add(new HashMap<String, String>() {{
                put(idFieldName(), instanceInfo.getName());
                put("host", instanceInfo.getHost());
                put("port", String.valueOf(instanceInfo.getPort()));
            }});
        });

        return ModelUtil.listData(result, pageNum, pageSize, fieldNames);
    }

    @Override
    public int totalSize() {
        return Main.getService(Registry.class).getAllAppNames().size()
                + 1;// +1 local instance
    }

    @Override
    public Map<String, String> showData(String id) {
        if (id.equals(DeployerConstants.INSTANCE_LOCAL)) return localInstance();

        Registry registry = Main.getService(Registry.class);
        InstanceInfo instanceInfo = registry.getInstanceInfo(id);
        if (instanceInfo == null) return null;
        return new HashMap<String, String>() {{
            put(idFieldName(), instanceInfo.getName());
            put("host", instanceInfo.getHost());
            put("port", String.valueOf(instanceInfo.getPort()));
        }};
    }

    private Map<String, String> localInstance() {
        return new HashMap<String, String>() {{
            put(idFieldName(), DeployerConstants.INSTANCE_LOCAL);
            put("host", "localhost");

            Config config = Main.getService(Config.class);
            Agent agent = config.getAgent();
            put("port", agent.isEnabled() ? String.valueOf(agent.getAgentPort()) : "--");
        }};
    }

    @Override
    public File downloadData(String id) {
        return new File(Main.getService(ModuleContext.class).getInstanceDir(), "logs");
    }

    @Override
    public Map<String, String> monitorData() {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();

        Map<String, String> data = new HashMap<>();
        data.put("osName", mxBean.getName());
        data.put("osVer", mxBean.getVersion());
        data.put("arch", mxBean.getArch());
        data.put("cpu", String.valueOf(mxBean.getAvailableProcessors()));

        long totalSpace = 0;
        long usableSpace = 0;
        for (File file : File.listRoots()) { // 所有磁盘计算总和
            totalSpace += file.getTotalSpace();
            usableSpace += file.getUsableSpace();
        }
        data.put("disk", maskGBytes(totalSpace));
        data.put("diskUsed", maskGBytes(totalSpace - usableSpace));

        double v = mxBean.getSystemLoadAverage() / mxBean.getAvailableProcessors();// mac 等系统
        data.put("cpuUsed", String.format("%.2f", v));

        return data;
    }

    public static String maskGBytes(long val) {
        double v = ((double) val) / 1024 / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }
}
