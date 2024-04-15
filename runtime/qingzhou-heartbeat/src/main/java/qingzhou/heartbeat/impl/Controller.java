package qingzhou.heartbeat.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyPairCipher;
import qingzhou.engine.ServiceRegister;
import qingzhou.engine.util.IPUtil;
import qingzhou.heartbeat.Heartbeat;
import qingzhou.http.Http;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppInfo;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.Registry;

import java.util.*;
import java.util.stream.Collectors;

public class Controller extends ServiceRegister<Heartbeat> {
    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Logger logger;
    private Registry registry;
    private CryptoService cryptoService;
    private Http http;
    private Json json;
    private Remote remote;

    @Override
    protected void startService() throws Exception {
        super.startService();
        ConfigService configService = moduleContext.getService(ConfigService.class);
        remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

        logger = moduleContext.getService(Logger.class);
        registry = moduleContext.getService(Registry.class);
        cryptoService = moduleContext.getService(CryptoService.class);
        http = moduleContext.getService(Http.class);
        json = moduleContext.getService(Json.class);

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
    protected void stopService() {
        super.stopService();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public Class<Heartbeat> serviceType() {
        return Heartbeat.class;
    }

    @Override
    protected Heartbeat serviceObject() {
        return () -> {
            try {
                return thisInstanceInfo().getKey();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void register() throws Exception {
        Collection<String> allInstanceId = registry.getAllInstanceId();
        if (allInstanceId.size() != 1) {
            throw new IllegalStateException(allInstanceId.stream().map(s -> registry.getInstanceInfo(s).id).collect(Collectors.joining()));
        }

        InstanceInfo instanceInfo = thisInstanceInfo();
        AppInfo[] appInfos = registry.getInstanceInfo(allInstanceId.iterator().next()).getAppInfos();
        instanceInfo.setAppInfos(appInfos);

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

    private InstanceInfo instanceInfo;

    private InstanceInfo thisInstanceInfo() throws Exception {
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo();
            instanceInfo.setId(UUID.randomUUID().toString().replace("-", ""));
            instanceInfo.setName(remote.getName());
            instanceInfo.setHost(remote.getHost() != null && !remote.getHost().isEmpty() ? remote.getHost() : Arrays.toString(IPUtil.getLocalIps().toArray(new String[0])));
            instanceInfo.setPort(remote.getPort());
            KeyPairCipher keyPairCipher = cryptoService.getKeyPairCipher(remote.getMaster().getPublicKey(), null);
            String key = keyPairCipher.encryptWithPublicKey(UUID.randomUUID().toString());
            instanceInfo.setKey(key);
        }
        return instanceInfo;
    }
}
