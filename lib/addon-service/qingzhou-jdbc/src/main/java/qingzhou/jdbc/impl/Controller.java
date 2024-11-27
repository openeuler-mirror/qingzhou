package qingzhou.jdbc.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.jdbc.Jdbc;

@Module
public class Controller implements ModuleActivator {

    @Override
    public void start(ModuleContext context) {
        context.registerService(Jdbc.class, ConnectionPoolBuilderImpl::new);
    }
}
