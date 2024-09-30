package qingzhou.agent.impl;

import java.util.Map;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.deployer.AppListener;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Json json;
    @Service
    private Http http;
    @Service
    private Logger logger;
    @Service
    private Deployer deployer;
    @Service
    private CryptoService cryptoService;

    private ProcessSequence sequence;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        Map<String, String> config = (Map<String, String>) moduleContext.getConfig();
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
                new qingzhou.agent.impl.Service(agentHost, agentPort, agentCipher, http, logger, json, deployer),
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
                || agentHost.equals("0.0.0.0")
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
