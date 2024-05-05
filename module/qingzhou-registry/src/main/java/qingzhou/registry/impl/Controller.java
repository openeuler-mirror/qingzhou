package qingzhou.registry.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Json json;

    @Override
    public void start(ModuleContext context) {
        context.registerService(Registry.class, new RegistryImpl(json));
    }
}
