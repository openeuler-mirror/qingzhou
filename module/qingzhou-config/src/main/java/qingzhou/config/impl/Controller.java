package qingzhou.config.impl;

import qingzhou.config.ConfigService;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.json.Json;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Json json;

    @Override
    public void start(ModuleContext context) {
        context.registerService(ConfigService.class, new ConfigServiceImpl(json));
    }
}
