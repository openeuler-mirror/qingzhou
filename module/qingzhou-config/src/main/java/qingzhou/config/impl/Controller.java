package qingzhou.config.impl;

import qingzhou.config.Config;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.json.Json;

import java.io.File;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Json json;

    @Override
    public void start(ModuleContext context) {
        context.registerService(Config.class,
                new JsonFileConfig(json, new File(context.getInstanceDir(), "qingzhou.json"))
        );
    }
}
