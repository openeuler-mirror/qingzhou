package qingzhou.core.agent.impl;

import java.util.Map;

import qingzhou.core.deployer.AppListener;
import qingzhou.core.deployer.Deployer;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.PairCipher;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Process;
import qingzhou.engine.util.pattern.ProcessSequence;

public class Controller implements Process {
    private final ModuleContext moduleContext;
    private ProcessSequence sequence;

    public Controller(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void exec() throws Throwable {
        Map<String, String> deployer = (Map<String, String>) ((Map<String, Object>) moduleContext.getConfig()).get("deployer");
        if (Boolean.parseBoolean(deployer.get("staticMode"))) {
            // 静态模式下，不能进行应用和实例的“卸载”和“添加”，注册服务也会关闭
            return;
        }

        Map<String, String> config = (Map<String, String>) ((Map<String, Object>) moduleContext.getConfig()).get("agent");
        if (config == null || !Boolean.parseBoolean(config.get("enabled"))) return;

        String agentHost = getAgentHost(config.get("agentHost"));
        int agentPort = Integer.parseInt(config.get("agentPort"));

        CryptoService cryptoService = moduleContext.getService(CryptoService.class);
        String generateKey = cryptoService.generateKey();

        PairCipher pairCipher = cryptoService.getPairCipher(config.get("masterKey"), null);
        String encryptedAgentKey = pairCipher.encryptWithPublicKey(generateKey);

        Cipher agentCipher = cryptoService.getCipher(generateKey);

        Heartbeat heartbeat = new Heartbeat(moduleContext, agentHost, agentPort, encryptedAgentKey, config);
        sequence = new ProcessSequence(
                () -> moduleContext.getService(Deployer.class).addAppListener(new AppListenerImpl(heartbeat)),
                new Service(moduleContext, agentCipher, agentHost, agentPort),
                heartbeat
        );
        sequence.exec();
    }

    @Override
    public void undo() {
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
