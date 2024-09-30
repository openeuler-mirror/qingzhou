package qingzhou.agent.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import qingzhou.crypto.MessageDigest;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Process;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;

class Heartbeat implements Process {
    private final Map<String, String> config;
    private final Json json;
    private final Deployer deployer;
    private final Logger logger;
    private final MessageDigest messageDigest;
    private final Http http;
    private final String agentHost;
    private final int agentPort;
    private final String encryptedAgentKey;

    Heartbeat(String agentHost, int agentPort, String encryptedAgentKey, Map<String, String> config, Json json, Deployer deployer, Logger logger, MessageDigest messageDigest, Http http) {
        this.config = config;
        this.json = json;
        this.deployer = deployer;
        this.logger = logger;
        this.messageDigest = messageDigest;
        this.http = http;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.encryptedAgentKey = encryptedAgentKey;
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
                register();
            }
        }, 2000, 1000 * 2);
    }

    @Override
    public void undo() {
        if (timer != null) {
            timer.cancel();
        }
    }

    void register() {
        List<AppInfo> appInfos = new ArrayList<>();
        for (String a : deployer.getAllApp()) {
            if (!DeployerConstants.APP_SYSTEM.equals(a)) {
                AppInfo appInfo = deployer.getApp(a).getAppInfo();
                appInfos.add(appInfo);
            }
        }
        appInfos.sort(Comparator.comparing(AppInfo::getName));
        thisInstanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));

        String registerData = json.toJson(thisInstanceInfo);

        HttpResponse response;
        String fingerprint = messageDigest.fingerprint(registerData);
        try {
            response = http.buildHttpClient().send(checkUrl, new HashMap<String, String>() {{
                put(DeployerConstants.CHECK_FINGERPRINT, fingerprint);
            }});
        } catch (Throwable e) {
            logger.warn("The registration check failed: " + e.getMessage());
            return;
        }

        boolean registered = false;
        if (response != null && response.getResponseCode() == 200) {
            Map resultMap;
            try {
                resultMap = json.fromJson(new String(response.getResponseBody(), DeployerConstants.ACTION_INVOKE_CHARSET), Map.class);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
                return;
            }
            List<Map<String, String>> dataList = (List<Map<String, String>>) resultMap.get(DeployerConstants.JSON_DATA);
            if (dataList != null && !dataList.isEmpty()) {
                String checkResult = dataList.get(0).get(fingerprint);
                registered = Boolean.parseBoolean(checkResult);
            }
        }
        if (registered) return;

        try {
            http.buildHttpClient().send(registerUrl, new HashMap<String, String>() {{
                put("doRegister", registerData);
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
        return instanceInfo;
    }
}
