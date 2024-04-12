package qingzhou.heartbeat.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyPairCipher;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.IPUtil;
import qingzhou.http.Http;
import qingzhou.logger.Logger;
import qingzhou.registry.InstanceInfo;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Controller implements Module {
    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Logger logger;
    private CryptoService cryptoService;
    private Http http;
    private Remote remote;

    @Override
    public void start(ModuleContext moduleContext) {
        ConfigService configService = moduleContext.getService(ConfigService.class);
        remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

        logger = moduleContext.getService(Logger.class);
        cryptoService = moduleContext.getService(CryptoService.class);
        http = moduleContext.getService(Http.class);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                register();
            }
        }, 2000, 1000 * 30);
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void register() {
        InstanceInfo instanceInfo = getInstanceInfo();
        instanceInfo.setAppInfos();

        try {
            http.buildHttpClient().send(master.get("url"), map);
        } catch (Throwable e) {
            logger.warn("An exception occurred during the registration process", e);
        }
    }

    private InstanceInfo instanceInfo;

    private InstanceInfo getInstanceInfo() throws Exception {
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
