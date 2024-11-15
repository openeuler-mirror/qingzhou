package qingzhou.agent.impl;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.deployer.AppListener;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.serializer.Serializer;

import java.util.Map;

@Module
public class Controller implements ModuleActivator {
    @Resource
    private Json json;
    @Resource
    private Serializer serializer;
    @Resource
    private Http http;
    @Resource
    private Logger logger;
    @Resource
    private Deployer deployer;
    @Resource
    private CryptoService cryptoService;

    private ProcessSequence sequence;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Map<String, String> config = moduleContext.getConfig();
        if (config == null || !Boolean.parseBoolean(config.get("enabled"))) return;

        String agentHost = getAgentHost(config.get("agentHost"));
        int agentPort = Integer.parseInt(config.get("agentPort"));

        String generateKey = cryptoService.generateKey();

        PairCipher pairCipher = cryptoService.getPairCipher(config.get("masterKey"), null);
        String encryptedAgentKey = pairCipher.encryptWithPublicKey(generateKey);

        Cipher agentCipher = cryptoService.getCipher(generateKey);

        Heartbeat heartbeat = new Heartbeat(agentHost, agentPort, encryptedAgentKey, config, json, deployer, logger, cryptoService.getMessageDigest(), http, moduleContext);
        sequence = new ProcessSequence(
                () -> deployer.addAppListener(new AppListenerImpl(heartbeat)),
                new qingzhou.agent.impl.Service(agentHost, agentPort, agentCipher, http, logger, json, deployer, serializer),
                heartbeat
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        if (sequence == null) return;

        sequence.undo();
    }

    private String getAgentHost(String agentHost) {
        if (agentHost == null
                || agentHost.isEmpty()
                || "0.0.0.0".equals(agentHost)
        ) {
            agentHost = Utils.getLocalIps().iterator().next();
        }
        return agentHost;
    }

    private static class AppListenerImpl implements AppListener {
        final Heartbeat heartbeat;

        AppListenerImpl(Heartbeat heartbeat) {
            this.heartbeat = heartbeat;
        }

        @Override
        public void onInstalled(String appName) {
            heartbeat.register();
        }

        @Override
        public void onUninstalled(String appName) {
            heartbeat.register();
        }
    }
}
