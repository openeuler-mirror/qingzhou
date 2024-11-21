package qingzhou.core.impl;


import qingzhou.crypto.CryptoService;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.engine.util.pattern.ProcessSequence;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.serializer.Serializer;

@Module
public class Controller implements ModuleActivator {
    @Resource
    public static Logger logger;
    @Resource
    private Json json;
    @Resource
    private CryptoService cryptoService;
    @Resource
    private Serializer serializer;
    @Resource
    private QrGenerator qrGenerator;
    @Resource
    private Http http;

    private ProcessSequence processSequence;

    @Override
    public void start(ModuleContext context) throws Exception {
        processSequence = new ProcessSequence(
                new qingzhou.core.config.impl.Controller(context),
                new qingzhou.core.registry.impl.Controller(context),
                new qingzhou.core.deployer.impl.Controller(context),
                new qingzhou.core.agent.impl.Controller(context),
                new qingzhou.core.console.impl.Controller(context)
        );
        processSequence.exec();
    }

    @Override
    public void stop() {
        processSequence.undo();
    }
}
