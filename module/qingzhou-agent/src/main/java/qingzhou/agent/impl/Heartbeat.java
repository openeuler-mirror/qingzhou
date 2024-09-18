package qingzhou.agent.impl;

import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Process;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;

import java.util.*;

class Heartbeat implements Process {
    private final Config config;
    private final Json json;
    private final Deployer deployer;
    private final Logger logger;
    private final CryptoService cryptoService;
    private final Http http;
    private final String agentHost;
    private final int agentPort;
    private final String agentKey;

    Heartbeat(String agentHost, int agentPort, String agentKey, Config config, Json json, Deployer deployer, Logger logger, CryptoService cryptoService, Http http) {
        this.config = config;
        this.json = json;
        this.deployer = deployer;
        this.logger = logger;
        this.cryptoService = cryptoService;
        this.http = http;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.agentKey = agentKey;
    }

    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private String checkUrl;
    private String registerUrl;
    private InstanceInfo thisInstanceInfo;

    @Override
    public void exec() {
        Agent agent = config.getAgent();
        if (agent == null || !agent.isEnabled()) return;

        String masterUrl = config.getAgent().getMasterUrl();
        if (masterUrl == null || masterUrl.trim().isEmpty()) {
            logger.error("Instance registration fails: \"masterUrl\" is not set correctly.");
            return;
        }
        if (masterUrl.endsWith("/")) {
            masterUrl = masterUrl.substring(0, masterUrl.length() - 1);
        }
        String baseUri = masterUrl + DeployerConstants.REST_PREFIX + "/" + DeployerConstants.JSON_VIEW + "/" + DeployerConstants.APP_SYSTEM + "/" + DeployerConstants.MODEL_MASTER + "/";
        checkUrl = baseUri + DeployerConstants.ACTION_CHECK;
        registerUrl = baseUri + DeployerConstants.ACTION_REGISTER;

        thisInstanceInfo = thisInstanceInfo();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    register();
                } catch (Exception e) {
                    logger.error("Failed to register with Master: " + e.getMessage());
                }
            }
        }, 2000, 1000 * 30);
    }

    @Override
    public void undo() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void register() throws Exception {
        List<AppInfo> appInfos = new ArrayList<>();
        for (String a : deployer.getAllApp()) {
            if (!DeployerConstants.APP_SYSTEM.equals(a)) {
                appInfos.add(deployer.getApp(a).getAppInfo());
            }
        }
        thisInstanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));
        String registerData = json.toJson(thisInstanceInfo);

        boolean registered = false;
        try {
            String fingerprint = cryptoService.getMessageDigest().fingerprint(registerData);
            HttpResponse response = http.buildHttpClient().send(checkUrl, new HashMap<String, String>() {{
                put(DeployerConstants.CHECK_FINGERPRINT, fingerprint);
            }});
            if (response.getResponseCode() == 200) {
                Map resultMap = json.fromJson(new String(response.getResponseBody(), DeployerConstants.ACTION_INVOKE_CHARSET), Map.class);
                List<Map<String, String>> dataList = (List<Map<String, String>>) resultMap.get(DeployerConstants.JSON_DATA);
                if (dataList != null && !dataList.isEmpty()) {
                    String checkResult = dataList.get(0).get(fingerprint);
                    registered = Boolean.parseBoolean(checkResult);
                }
            }
        } catch (Throwable e) {
            logger.warn("An exception occurred during the registration process", e);
        }
        if (registered) return;

        http.buildHttpClient().send(registerUrl, new HashMap<String, String>() {{
            put("doRegister", registerData);
        }});
    }

    private InstanceInfo thisInstanceInfo() {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setName(UUID.randomUUID().toString().replace("-", ""));
        instanceInfo.setHost(agentHost);
        instanceInfo.setPort(agentPort);
        instanceInfo.setKey(agentKey);
        return instanceInfo;
    }
}
