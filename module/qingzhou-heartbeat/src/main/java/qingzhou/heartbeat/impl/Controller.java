package qingzhou.heartbeat.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.io.IOException;
import java.util.*;

@Module
public class Controller implements ModuleActivator {
    @Service
    private ConfigService configService;
    @Service
    private Logger logger;
    @Service
    private CryptoService cryptoService;
    @Service
    private Http http;
    @Service
    private Json json;
    @Service
    private Deployer deployer;

    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Remote remote;


    @Override
    public void start(ModuleContext context) throws IOException {
        remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

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
        InstanceInfo instanceInfo = deployer.getInstanceInfo();
        List<AppInfo> appInfos = new ArrayList<>();
        for (String a : deployer.getAllApp()) {
            appInfos.add(deployer.getApp(a).getAppInfo());
        }
        instanceInfo.setAppInfos(appInfos.toArray(new AppInfo[0]));

        String registerData = json.toJson(instanceInfo);

        boolean doRegister = false;
        try {
            String fingerprint = cryptoService.getMessageDigest().fingerprint(registerData);
            HttpResponse response = http.buildHttpClient().send(remote.getMaster().getUrl(), new HashMap<String, String>() {{
                put(Registry.PARAMETER_FINGERPRINT, fingerprint);
            }});
            if (response.getResponseCode() == 200) {
                Map<String, String> resultMap = json.fromJson(response.getResponseBody(), Map.class);
                String checkResult = resultMap.get(fingerprint);
                doRegister = !Boolean.parseBoolean(checkResult);
            }
        } catch (Throwable e) {
            logger.warn("An exception occurred during the registration process", e);
        }
        if (!doRegister) return;

        http.buildHttpClient().send(remote.getMaster().getUrl(), new HashMap<String, String>() {{
            put(Registry.PARAMETER_DO_REGISTER, registerData);
        }});
    }
}
