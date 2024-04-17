package qingzhou.heartbeat.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.*;

public class Controller implements Module {
    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Logger logger;
    private Registry registry;
    private Deployer deployer;
    private CryptoService cryptoService;
    private Http http;
    private Json json;
    private Remote remote;


    @Override
    public void start(ModuleContext moduleContext) {
        ConfigService configService = moduleContext.getService(ConfigService.class);
        remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

        logger = moduleContext.getService(Logger.class);
        registry = moduleContext.getService(Registry.class);
        cryptoService = moduleContext.getService(CryptoService.class);
        http = moduleContext.getService(Http.class);
        json = moduleContext.getService(Json.class);
        deployer = moduleContext.getService(Deployer.class);

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
        InstanceInfo instanceInfo = registry.thisInstanceInfo();
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
                HashMap<String, String> resultMap = json.fromJson(response.getResponseBody(), HashMap.class);
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
