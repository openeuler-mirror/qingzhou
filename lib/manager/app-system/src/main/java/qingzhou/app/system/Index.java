package qingzhou.app.system;

import qingzhou.api.*;
import qingzhou.api.type.Dashboard;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.config.Console;
import qingzhou.config.Jmx;
import qingzhou.config.Security;
import qingzhou.config.Web;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.App;
import qingzhou.core.deployer.*;
import qingzhou.core.registry.*;
import qingzhou.engine.util.Utils;

import java.util.*;

@Model(code = DeployerConstants.MODEL_INDEX, icon = "home",
        entrance = Dashboard.ACTION_DASHBOARD,
        name = {"主页", "en:Home"},
        info = {"查看 Qingzhou 平台的相关信息。", "en:Check out the relevant information of Qingzhou platform."})
public class Index extends ModelBase implements Dashboard {
    @ModelField(
            name = {"平台名称", "en:Platform Name"},
            info = {"Qingzhou 平台的名称。", "en:The name of Qingzhou platform."})
    public String name;

    @ModelField(
            name = {"平台版本", "en:Platform Version"},
            info = {"Qingzhou 平台的版本。", "en:This version of this Qingzhou platform."})
    public String version;

    @ModelField(
            name = {"Java 环境", "en:Java Env"},
            info = {"运行 Qingzhou 实例的 Java 环境。", "en:The Java environment in which Qingzhou instance is running."})
    public String javaHome;

    @ModelAction(
            code = Show.ACTION_SHOW,
            name = {"主页", "en:Home"},
            info = {"查看 Qingzhou 平台的相关信息。",
                    "en:View Qingzhou platform information."})
    public void show(Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<String, String>() {{
            put("name", "Qingzhou（轻舟）");
            put("version", getAppContext().getPlatformVersion());
            put("javaHome", System.getProperty("java.home"));
        }});
    }

    @ModelAction(// NOTE: 这个方法用作是 Login 成功后 跳过的
            code = DeployerConstants.ACTION_INDEX,
            name = {"主页", "en:Home"},
            info = {"进入 Qingzhou 平台的主页。",
                    "en:View Qingzhou platform information."})
    public void index(Request request) throws Exception {
        show(request);
    }

    @Override
    public void dashboardData(String id, DataBuilder builder) {
        // 基本信息
        Lang lang = getAppContext().getCurrentRequest().getLang();
        Basic basic = builder.buildData(Basic.class);
        basic.title(I18nTool.retrieveI18n(new String[]{"基本信息", "en:Basic Information"}).get(lang));
        Deployer deployer = Main.getService(Deployer.class);
        App app = deployer.getApp(DeployerConstants.APP_SYSTEM);
        ModelInfo modelInfo = app.getAppInfo().getModelInfo(DeployerConstants.MODEL_INDEX);
        basic.addData(I18nTool.retrieveI18n(modelInfo.getModelFieldInfo("name").getName()).get(lang), "Qingzhou（轻舟）");
        basic.addData(I18nTool.retrieveI18n(modelInfo.getModelFieldInfo("version").getName()).get(lang), getAppContext().getPlatformVersion());
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

        basic.addData(I18nTool.retrieveI18n(new String[]{"实例数量", "en:Number Of Instance"}).get(lang), String.valueOf(allInstanceNames.size()));
        basic.addData(I18nTool.retrieveI18n(new String[]{"应用数量", "en:Number Of App"}).get(lang), String.valueOf(appNames.size()));
        Console console = Main.getConsole();
        Web web = console.getWeb();
        basic.addData(I18nTool.retrieveI18n(new String[]{"Web 服务端口", "en:Web Service Port"}).get(lang), String.valueOf(web.getPort()));
        Jmx jmx = console.getJmx();
        if (jmx.isEnabled()) {
            basic.addData(I18nTool.retrieveI18n(new String[]{"JMX 服务端口", "en:JMX Service Port"}).get(lang), String.valueOf(jmx.getPort()));
        } else {
            basic.addData(I18nTool.retrieveI18n(new String[]{"JMX 服务", "en:JMX Service"}).get(lang), I18nTool.retrieveI18n(new String[]{"未启用", "en:Not Enabled"}).get(lang));
        }
        Security security = console.getSecurity();
        String trustedIp = security.getTrustedIp();
        basic.addData(I18nTool.retrieveI18n(new String[]{"信任 IP", "en:Trusted IP"}).get(lang), Utils.isBlank(trustedIp) ? I18nTool.retrieveI18n(new String[]{"未设置", "en:Not Set"}).get(lang) : trustedIp);
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

    @Override
    public int period() {
        return 5000;
    }
}
