package qingzhou.jdbc.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.jdbc.Jdbc;

import java.util.ArrayList;
import java.util.List;

@Module
public class Controller implements ModuleActivator {
    static final List<Runnable> shutdownHookList = new ArrayList<>();

    @Override
    public void start(ModuleContext context) {
        context.registerService(Jdbc.class, ConnectionPoolBuilderImpl::new);
    }

    @Override
    public void stop() {
        new ArrayList<>(shutdownHookList).forEach(Runnable::run);
    }
}
