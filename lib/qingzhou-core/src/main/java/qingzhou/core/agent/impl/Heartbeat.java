package qingzhou.core.agent.impl;

import qingzhou.core.registry.AppInfo;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.DeployerConstants;
import qingzhou.core.registry.InstanceInfo;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.pattern.Process;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.util.*;

class Heartbeat implements Process {
    private final ModuleContext moduleContext;
    private final Map<String, String> config;
    private final String agentHost;
    private final int agentPort;
    private final String encryptedAgentKey;

    Heartbeat(ModuleContext moduleContext, String agentHost, int agentPort, String encryptedAgentKey, Map<String, String> config) {
        this.config = config;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.encryptedAgentKey = encryptedAgentKey;
        this.moduleContext = moduleContext;
    }

    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private String checkUrl;
    private String registerUrl;
    private InstanceInfo thisInstanceInfo;

    @Override
    public void exec() {
        String masterUrl = config.get("masterUrl");
        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            moduleContext.getService(Logger.class).error("Instance registration fails: \"masterUrl\" is not set correctly.");
            return;
        }
        if (masterUrl.endsWith("/")) {
            masterUrl = masterUrl.substring(0, masterUrl.length() - 1);
        }
        String baseUri = masterUrl + DeployerConstants.REST_PREFIX + "/" + DeployerConstants.JSON_VIEW_FLAG + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_MASTER + "/";
        checkUrl = baseUri + DeployerConstants.ACTION_CHECK;
        registerUrl = baseUri + DeployerConstants.ACTION_REGISTER;

        thisInstanceInfo = thisInstanceInfo();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                register();
            }
        }, 2000, 1000 * Long.parseLong(config.get("registerInterval")));
    }

    @Override
    public void undo() {
        if (timer != null) {
            timer.cancel();
        }
    }

    void register() {
        try {
            register0();
        } catch (Exception e) {
            moduleContext.getService(Logger.class).error(e.getMessage(), e);
        }
    }

    void register0() throws Exception {
        List<AppInfo> appInfos = new ArrayList<>();
        Deployer deployer = moduleContext.getService(Deployer.class);
        for (String a : deployer.getAllApp()) {
            if (!DeployerConstants.APP_SYSTEM.equals(a)) {
                AppInfo appInfo = deployer.getApp(a).getAppInfo();
                appInfos.add(appInfo);
            }
        }
        appInfos.sort(Comparator.comparing(AppInfo::getName));
        thisInstanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));

        String registerData = moduleContext.getService(Json.class).toJson(thisInstanceInfo);

        HttpResponse response;
        String fingerprint = moduleContext.getService(CryptoService.class).getMessageDigest().fingerprint(registerData);
        Http http = moduleContext.getService(Http.class);
        Logger logger = moduleContext.getService(Logger.class);
        try {
            response = http.buildHttpClient().post(checkUrl, new HashMap<String, String>() {{
                put(DeployerConstants.CHECK_FINGERPRINT, fingerprint);
            }});
        } catch (Throwable e) {
            logger.warn("The registration check failed: " + e.getMessage());
            return;
        }

        boolean registered = false;
        if (response != null && response.getResponseCode() == 200) {
            String checkResult = new String(response.getResponseBody(), DeployerConstants.ACTION_INVOKE_CHARSET);
            registered = Boolean.parseBoolean(checkResult);
        }
        if (registered) return;

        try {
            http.buildHttpClient().post(registerUrl, new HashMap<String, String>() {{
                put(DeployerConstants.DO_REGISTER, registerData);
            }});
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private InstanceInfo thisInstanceInfo() {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setName(UUID.randomUUID().toString().replace("-", ""));
        instanceInfo.setHost(agentHost);
        instanceInfo.setPort(agentPort);
        instanceInfo.setKey(encryptedAgentKey);
        instanceInfo.setVersion(moduleContext.getPlatformVersion());
        return instanceInfo;
    }
}
