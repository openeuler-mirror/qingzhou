package qingzhou.app.system.business;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import qingzhou.api.Lang;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.Response;
import qingzhou.api.type.Dashboard;
import qingzhou.api.type.Monitor;
import qingzhou.app.system.Index;
import qingzhou.app.system.Main;
import qingzhou.config.console.Console;
import qingzhou.config.console.Jmx;
import qingzhou.config.console.Security;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.*;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.InstanceInfo;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.core.registry.Registry;
import qingzhou.engine.util.Utils;

@Model(code = "overview", icon = "dashboard",
        entrance = Dashboard.ACTION_DASHBOARD,
        menu = Main.Business, order = "1",
        name = {"概览", "en:Overview"},
        info = {"查看 Qingzhou 平台的概览信息。", "en:Check out the overview information of Qingzhou platform."})
public class Overview extends ModelBase implements Dashboard {

    @Override
    public int period() {
        return 3000;
    }

    private Map<String, String> allInstanceHosts() {
        Map<String, String> instances = new LinkedHashMap<>();
        instances.put(DeployerConstants.INSTANCE_LOCAL, "localhost");
        Registry registry = Main.getService(Registry.class);
        registry.getAllInstanceNames().forEach(s -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(s);
            instances.put(instanceInfo.getName(), instanceInfo.getHost());
        });
        return instances;
    }

    @Override
    public void dashboardData(String id, DataBuilder builder) {
        Lang lang = getAppContext().getCurrentRequest().getLang();
        Map<String, String> allInstanceHosts = allInstanceHosts();
        Map<String, Map<String, String>> monitorData = monitorData(allInstanceHosts.keySet().toArray(new String[0]));
        Set<String> deleteIds = allInstanceHosts.keySet().stream().filter(s -> !monitorData.containsKey(s)).collect(Collectors.toSet());
        deleteIds.forEach(allInstanceHosts::remove);

        String[] allAppIds = App.allIds(null);

        // 基本信息
        Basic basic = getBasic(builder, lang);
        builder.addData(new Basic[]{basic});

        // 仪表板
        builder.addData(getGauge(builder, allInstanceHosts, monitorData, lang));

        // 份额比例图
        builder.addData(getShareDataset(builder, allInstanceHosts, monitorData, lang));

        // 热力图
        builder.addData(getMatrixHeatmap(builder, lang));

        // 追加基本数据
        basic.addData(I18nTool.retrieveI18n(new String[]{"实例数量", "en:Number Of Instance"}).get(lang), String.valueOf(allInstanceHosts.size()));
        basic.addData(I18nTool.retrieveI18n(new String[]{"应用数量", "en:Number Of App"}).get(lang), String.valueOf(allAppIds.length));
    }

    private ShareDataset[] getShareDataset(DataBuilder builder, Map<String, String> allInstanceHosts, Map<String, Map<String, String>> data, Lang lang) {
        List<ShareDataset> datasetList = new ArrayList<>();

        AppInfo appInfo = Main.getService(Deployer.class).getApp(DeployerConstants.APP_SYSTEM).getAppInfo();
        ModelInfo modelInfo = appInfo.getModelInfo(DeployerConstants.MODEL_INSTANCE);
        String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
        for (String fieldName : monitorFieldNames) {
            ModelFieldInfo monitorField = modelInfo.getModelFieldInfo(fieldName);
            if (!monitorField.isNumeric()) continue;

            ShareDataset dataset = builder.buildData(ShareDataset.class);
            dataset.title(I18nTool.retrieveI18n(monitorField.getName()).get(lang));
            for (Map.Entry<String, String> entry : allInstanceHosts.entrySet()) {
                Map<String, String> monitor = data.get(entry.getKey());

                String host = entry.getValue();
                dataset.addData(host, monitor.get(fieldName));
            }
            datasetList.add(dataset);
        }

        return datasetList.toArray(new ShareDataset[0]);
    }

    private Gauge[] getGauge(DataBuilder builder, Map<String, String> allInstanceHosts, Map<String, Map<String, String>> data, Lang lang) {
        List<Gauge> gauges = new ArrayList<>();

        Gauge cpu = buildGauge(builder, "cpuUsed", "cpu", "%", lang);
        Gauge heap = buildGauge(builder, "heapUsed", "heapCommitted", "MB", lang);
        Gauge disk = buildGauge(builder, "diskUsed", "disk", "GB", lang);

        for (Map.Entry<String, String> entry : allInstanceHosts.entrySet()) {
            String host = entry.getValue();
            Map<String, String> monitor = data.get(entry.getKey());

            cpu.addData(new String[]{host, monitor.get("cpuUsed"), monitor.get("cpu")});
            heap.addData(new String[]{host, monitor.get("heapUsed"), monitor.get("heapCommitted")});
            disk.addData(new String[]{host, monitor.get("diskUsed"), monitor.get("disk")});
        }

        gauges.add(cpu);
        gauges.add(heap);
        gauges.add(disk);

        return gauges.toArray(new Gauge[0]);
    }

    private Gauge buildGauge(DataBuilder builder, String usedField, String maxField, String unit, Lang lang) {
        Gauge gauge = builder.buildData(Gauge.class);

        AppInfo appInfo = Main.getService(Deployer.class).getApp(DeployerConstants.APP_SYSTEM).getAppInfo();
        ModelInfo modelInfo = appInfo.getModelInfo(DeployerConstants.MODEL_INSTANCE);

        ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(usedField);
        String title = I18nTool.retrieveI18n(fieldInfo.getName()).get(lang);
        gauge.info(I18nTool.retrieveI18n(fieldInfo.getInfo()).get(lang))
                .title(title);

        String ipKey = I18nTool.retrieveI18n(modelInfo.getModelFieldInfo("host").getName()).get(lang);
        String maxKey = I18nTool.retrieveI18n(modelInfo.getModelFieldInfo(maxField).getName()).get(lang);

        gauge.fields(new String[]{ipKey, title, maxKey}).usedKey(title).maxKey(maxKey).unit(unit);
        return gauge;
    }

    private MatrixHeatmap[] getMatrixHeatmap(DataBuilder builder, Lang lang) {
        Registry registry = Main.getService(Registry.class);
        MatrixHeatmap matrixHeatmap = builder.buildData(MatrixHeatmap.class);
        matrixHeatmap.title(I18nTool.retrieveI18n(new String[]{"应用分布点位", "en:App Distribution Points"}).get(lang));
        matrixHeatmap.xAxisName(I18nTool.retrieveI18n(new String[]{"实例", "en:Instance"}).get(lang));
        matrixHeatmap.yAxisName(I18nTool.retrieveI18n(new String[]{"应用", "en:App"}).get(lang));

        registry.getAllInstanceNames().forEach(instanceName -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(instanceName);
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                matrixHeatmap.addData(instanceInfo.getHost(), appInfo.getName(), 1);
            }
        });

        Deployer deployer = Main.getService(Deployer.class);
        List<String> localApps = deployer.getLocalApps();
        localApps.stream()
                .filter(appName -> !DeployerConstants.APP_SYSTEM.equals(appName))
                .forEach(appName -> matrixHeatmap.addData(DeployerConstants.INSTANCE_LOCAL, appName, 1));
        return new MatrixHeatmap[]{matrixHeatmap};
    }

    private Basic getBasic(DataBuilder builder, Lang lang) {
        Basic basic = builder.buildData(Basic.class);
        basic.title(I18nTool.retrieveI18n(new String[]{"基本信息", "en:Basic Information"}).get(lang));
        Deployer deployer = Main.getService(Deployer.class);
        AppManager appManager = deployer.getApp(DeployerConstants.APP_SYSTEM);
        ModelInfo modelInfo = appManager.getAppInfo().getModelInfo(DeployerConstants.MODEL_INDEX);
        String nameField = "name";
        basic.addData(I18nTool.retrieveI18n(modelInfo.getModelFieldInfo(nameField).getName()).get(lang), Index.qzInfo.get(nameField));
        String versionField = "version";
        basic.addData(I18nTool.retrieveI18n(modelInfo.getModelFieldInfo(versionField).getName()).get(lang), Index.qzInfo.get(versionField));
        Console console = Main.getConsole();
        basic.addData(I18nTool.retrieveI18n(new String[]{"Web 服务端口", "en:Web Service Port"}).get(lang), String.valueOf(console.getPort()));
        Jmx jmx = console.getJmx();
        if (jmx.isEnabled()) {
            basic.addData(I18nTool.retrieveI18n(new String[]{"JMX 服务端口", "en:JMX Service Port"}).get(lang), String.valueOf(jmx.getPort()));
        } else {
            basic.addData(I18nTool.retrieveI18n(new String[]{"JMX 服务", "en:JMX Service"}).get(lang), I18nTool.retrieveI18n(new String[]{"未启用", "en:Not Enabled"}).get(lang));
        }
        Security security = console.getSecurity();
        String trustedIp = security.getTrustedIp();
        basic.addData(I18nTool.retrieveI18n(new String[]{"信任 IP", "en:Trusted IP"}).get(lang), Utils.isBlank(trustedIp) ? I18nTool.retrieveI18n(new String[]{"未设置", "en:Not Set"}).get(lang) : trustedIp);
        return basic;
    }

    private Map<String, Map<String, String>> monitorData(String[] allInstanceIds) {
        RequestImpl requestImpl = new RequestImpl();
        requestImpl.setAppName(DeployerConstants.APP_SYSTEM);
        requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
        requestImpl.setActionName(Monitor.ACTION_MONITOR);
        requestImpl.setResponse(new ResponseImpl());
        Map<String, Response> invokeOnInstances = Main.getService(ActionInvoker.class)
                .invokeAll(requestImpl, allInstanceIds);

        Map<String, Map<String, String>> monitorData = new LinkedHashMap<>();
        for (String instanceId : allInstanceIds) {
            ResponseImpl response = (ResponseImpl) invokeOnInstances.get(instanceId);
            Map<String, String> monitor = (Map<String, String>) response.getInternalData();
            if (monitor != null) {
                monitorData.put(instanceId, monitor);
            }
        }
        return monitorData;
    }
}
