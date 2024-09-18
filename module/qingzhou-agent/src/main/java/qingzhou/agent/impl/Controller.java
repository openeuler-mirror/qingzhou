package qingzhou.agent.impl;

import qingzhou.config.Agent;
import qingzhou.config.Config;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.CryptoService;
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
    private Config config;
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
        Agent agent = config.getAgent();
        if (agent == null) return;
        String agentHost = getAgentHost(agent);
        int agentPort = agent.getAgentPort();
        String agentKey = getAgentKey(agent);
        Cipher agentCipher = cryptoService.getCipher(agentKey);

        sequence = new ProcessSequence(
                new qingzhou.agent.impl.Service(agentHost, agentPort, agentCipher, config, http, logger, json, deployer),
                new Heartbeat(agentHost, agentPort, agentKey, config, json, deployer, logger, cryptoService, http)
        );
        sequence.exec();
    }

    @Override
    public void stop() {
        sequence.undo();
    }

    private String getAgentHost(Agent agent) {
        String agentHost = agent.getAgentHost();
        if (agentHost == null
                || agentHost.isEmpty()
                || agentHost.equals("0.0.0.0")
        ) {
            agentHost = Utils.getLocalIps().iterator().next();
        }
        return agentHost;
    }

    private String getAgentKey(Agent agent) {
        return agent.getAgentKey() == null || agent.getAgentKey().isEmpty()
                ? cryptoService.generateKey()
                : agent.getAgentKey();
    }
}
