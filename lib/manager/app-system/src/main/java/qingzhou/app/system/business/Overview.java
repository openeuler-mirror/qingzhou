package qingzhou.app.system.business;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.App;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.I18nTool;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
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
        return 5000;
    }

    @Override
    public void dashboardData(String id, DataBuilder builder) {
        // 基本信息
        Lang lang = getAppContext().getCurrentRequest().getLang();
        Basic basic = builder.buildData(Basic.class);
        basic.title(I18nTool.retrieveI18n(new String[]{"基本信息", "en:Basic Information"}).get(lang));
        Deployer deployer = Main.getService(Deployer.class);
        qingzhou.core.deployer.App app = deployer.getApp(DeployerConstants.APP_SYSTEM);
        ModelInfo modelInfo = app.getAppInfo().getModelInfo(DeployerConstants.MODEL_INDEX);
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
        builder.addData(new Basic[]{basic});

        // 应用
        Registry registry = Main.getService(Registry.class);
        List<String> allInstanceNames = registry.getAllInstanceNames();
        Set<String> appNames = new HashSet<>();
        MatrixHeatmap matrixHeatmap = builder.buildData(MatrixHeatmap.class);
        matrixHeatmap.title(I18nTool.retrieveI18n(new String[]{"实例应用部署关系", "en:Instance application deployment relationship"}).get(lang));
        matrixHeatmap.xAxisName(I18nTool.retrieveI18n(new String[]{"实例", "en:Instance"}).get(lang));
        matrixHeatmap.yAxisName(I18nTool.retrieveI18n(new String[]{"应用", "en:App"}).get(lang));

        allInstanceNames.forEach(instanceName -> {
            InstanceInfo instanceInfo = registry.getInstanceInfo(instanceName);
            for (AppInfo appInfo : instanceInfo.getAppInfos()) {
                appNames.add(appInfo.getName());
                matrixHeatmap.addData(instanceName, appInfo.getName(), 1);
            }
        });

        List<String> localApps = deployer.getLocalApps();
        localApps.stream()
                .filter(appName -> !DeployerConstants.APP_SYSTEM.equals(appName))
                .forEach(appName -> {
                    appNames.add(appName);
                    matrixHeatmap.addData(DeployerConstants.INSTANCE_LOCAL, appName, 1);
                });

        builder.addData(new MatrixHeatmap[]{matrixHeatmap});

        // 实例监控信息
        allInstanceNames.add(0, DeployerConstants.INSTANCE_LOCAL);
        Map<String, ResponseImpl> monitors = (Map<String, ResponseImpl>) (Map<?, ?>) monitorData(allInstanceNames);
        LineChart[] lineCharts = monitors.entrySet().stream()
                .map(entry -> createLineChart(entry.getKey(), entry.getValue(), app, lang, builder))
                .filter(Objects::nonNull)
                .toArray(LineChart[]::new);

        builder.addData(lineCharts);

        // 追加基本数据
        basic.addData(I18nTool.retrieveI18n(new String[]{"实例数量", "en:Number Of Instance"}).get(lang), String.valueOf(allInstanceNames.size()));
        basic.addData(I18nTool.retrieveI18n(new String[]{"应用数量", "en:Number Of App"}).get(lang), String.valueOf(appNames.size()));
    }

    private LineChart createLineChart(String instanceName, ResponseImpl response, App app, Lang lang, DataBuilder builder) {
        Map<String, String> internalData = (Map<String, String>) response.getInternalData();
        if (internalData == null) {
            return null;
        }
        LineChart lineChart = builder.buildData(LineChart.class);
        lineChart.title(instanceName);
        ModelInfo instanceModel = app.getAppInfo().getModelInfo(DeployerConstants.MODEL_INSTANCE);
        for (String monitorFieldName : instanceModel.getMonitorFieldNames()) {
            ModelFieldInfo modelFieldInfo = instanceModel.getModelFieldInfo(monitorFieldName);
            if (!modelFieldInfo.isNumeric()) {
                continue;
            }
            String data = internalData.get(monitorFieldName);
            if (!Utils.isBlank(data)) {
                lineChart.addData(I18nTool.retrieveI18n(modelFieldInfo.getName()).get(lang), data);
            }
        }
        return lineChart;
    }

    private Map<String, Response> monitorData(List<String> instanceNames) {
        RequestImpl requestImpl = new RequestImpl();
        requestImpl.setAppName(DeployerConstants.APP_SYSTEM);
        requestImpl.setModelName(DeployerConstants.MODEL_AGENT);
        requestImpl.setActionName(Monitor.ACTION_MONITOR);
        requestImpl.setResponse(new ResponseImpl());
        return Main.getService(ActionInvoker.class)
                .invokeOnInstances(requestImpl, instanceNames.toArray(new String[0]));
    }
}
