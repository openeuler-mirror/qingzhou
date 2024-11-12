package qingzhou.config.impl;

import java.io.File;

import qingzhou.config.Config;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Resource;
import qingzhou.json.Json;


@Module
public class Controller implements ModuleActivator {
    @Resource
    private Json json;

    @Override
    public void start(ModuleContext context) {
        context.registerService(Config.class,
                new JsonFileConfig(json, new File(new File(context.getInstanceDir(), "conf"), "qingzhou.json"))
        );
    }
}
