package qingzhou.heartbeat.impl;

import qingzhou.config.ConfigService;
import qingzhou.config.Master;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyPairCipher;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.IPUtil;
import qingzhou.logger.Logger;
import qingzhou.registry.InstanceInfo;

import java.util.*;

public class Controller implements Module {
    // 定时器设计目的：解决 master 未启动或者宕机重启等引起的注册失效问题
    private Timer timer;
    private Logger logger;
    private Remote remote;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        logger = moduleContext.getService(Logger.class);
        ConfigService configService = moduleContext.getService(ConfigService.class);
        remote = configService.getConfig().getRemote();
        if (!remote.isEnabled()) return;

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
        Map<String, String> map = new HashMap<>();
        map.put("nodeIp", remoteHost == null ? remoteHost : String.join(",", IPUtil.getLocalIps()));
        map.put("nodePort", String.valueOf(remotePort));
        map.put("apps", String.join(",", moduleContext.getService(AppManager.class).getApps()));
        // 获取master公钥，计算堆成密钥
        CryptoService cryptoService = moduleContext.getService(CryptoService.class);
        String remoteKey = config.getKey(Config.remoteKeyName);
        if (remoteKey == null) {
            // remoteKey = cryptoService.generateKey();
            // config.writeKey(Config.remoteKeyName, remoteKey);// todo
            throw new RuntimeException("not supported");
        }
        KeyPairCipher keyPairCipher = cryptoService.getKeyPairCipher(master.get(Config.remotePublicKeyName), null);
        map.put("key", keyPairCipher.encryptWithPublicKey(remoteKey));

        for (Master master : remote.getMaster()) {
            try {
                HttpClient.seqHttp(master.get("url"), map);
            } catch (Throwable e) {
                logger.warn("An exception occurred during the registration process", e);
            }
        }
    }

    private InstanceInfo instanceInfo;

    private InstanceInfo computeInstanceInfo() {
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo(
                    UUID.randomUUID().toString().replace("-", ""),
                    remote.getName(),
                    remote.getHost() != null && !remote.getHost().isEmpty() ? remote.getHost() : Arrays.toString(IPUtil.getLocalIps().toArray(new String[0])),
                    remote.getPort(),

                    );
        }
        return instanceInfo;
    }
}
