package qingzhou.json.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.json.Json;

@Module
public class Controller implements ModuleActivator {
    @Override
    public void start(ModuleContext context) {
        context.registerService(Json.class, new JsonImpl());
    }
}
