package qingzhou.serializer.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.serializer.Serializer;
import qingzhou.serializer.impl.java.JavaSerializer;

@Module
public class Controller implements ModuleActivator {
    @Override
    public void start(ModuleContext context) {
        context.registerService(Serializer.class, new JavaSerializer());
    }
}
