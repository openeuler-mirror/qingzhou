package qingzhou.uml.impl;


import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.uml.Uml;

@Module
public class Controller implements ModuleActivator {
    @Override
    public void start(ModuleContext context) {
        context.registerService(Uml.class, new UmlImpl());
    }
}
