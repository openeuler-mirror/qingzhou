package qingzhou.heartbeat.impl;

import qingzhou.agent.AgentService;
import qingzhou.config.ConfigService;
import qingzhou.config.Heartbeat;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.engine.util.crypto.KeyPairCipher;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;

import java.io.IOException;
import java.util.*;

@Module
public class Controller implements ModuleActivator {
    @Service
    private ConfigService configService;
    @Service
    private Logger logger;
    @Service
    private Http http;
    @Service
    private Json json;
    @Service
    private Deployer deployer;
    @Service
    private AgentService agentService;

    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Heartbeat heartbeat;
    private InstanceInfo thisInstanceInfo;

    @Override
    public void start(ModuleContext context) throws IOException {
        heartbeat = configService.getModule().getHeartbeat();
        if (!heartbeat.isEnabled()) return;

        thisInstanceInfo = thisInstanceInfo();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    register();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, 2000, 1000 * 30);
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void register() throws Exception {
        List<AppInfo> appInfos = new ArrayList<>();
        for (String a : deployer.getAllApp()) {
            appInfos.add(deployer.getApp(a).getAppInfo());
        }
        thisInstanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));

        String registerData = json.toJson(thisInstanceInfo);

        boolean doRegister = false;
        try {
            String fingerprint = CryptoServiceFactory.getInstance().getMessageDigest().fingerprint(registerData);
            HttpResponse response = http.buildHttpClient().send(heartbeat.getMasterUrl(), new HashMap<String, String>() {{
                put("fingerprint", fingerprint);
            }});
            if (response.getResponseCode() == 200) {
                Map resultMap = json.fromJson(response.getResponseBody(), Map.class);
                String checkResult = (String) resultMap.get(fingerprint);
                doRegister = !Boolean.parseBoolean(checkResult);
            }
        } catch (Throwable e) {
            logger.warn("An exception occurred during the registration process", e);
        }
        if (!doRegister) return;

        http.buildHttpClient().send(heartbeat.getMasterUrl(), new HashMap<String, String>() {{
            put("doRegister", registerData);
        }});
    }

    private InstanceInfo thisInstanceInfo() {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setId(UUID.randomUUID().toString().replace("-", ""));
        instanceInfo.setHost(agentService.getAgentHost().equals("0.0.0.0")
                ? Arrays.toString(Utils.getLocalIps().toArray(new String[0]))
                : agentService.getAgentHost());
        instanceInfo.setPort(agentService.getAgentPort());

        KeyPairCipher keyPairCipher;
        try {
            keyPairCipher = CryptoServiceFactory.getInstance().getKeyPairCipher(heartbeat.getMasterKey(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String key = keyPairCipher.encryptWithPublicKey(agentService.getAgentKey());
        instanceInfo.setKey(key);
        return instanceInfo;
    }
}
