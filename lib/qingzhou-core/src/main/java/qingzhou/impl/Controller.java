package qingzhou.impl;


import qingzhou.crypto.CryptoService;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.engine.util.pattern.ProcessPattern;
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

    private ProcessPattern processPattern;

    @Override
    public void start(ModuleContext context) throws Throwable {
        processPattern = new ProcessPattern(
                new qingzhou.core.registry.impl.Controller(context),
                new qingzhou.core.deployer.impl.Controller(context),
                new qingzhou.core.agent.impl.Controller(context),
                new qingzhou.core.console.impl.Controller(context)
        );
        processPattern.run();
    }

    @Override
    public void stop() {
        processPattern.completed();
    }
}
