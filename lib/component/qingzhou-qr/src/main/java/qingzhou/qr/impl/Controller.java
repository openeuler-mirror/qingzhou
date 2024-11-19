package qingzhou.qr.impl;


import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.qr.QrGenerator;

@Module
public class Controller implements ModuleActivator {

    @Override
    public void start(ModuleContext context) {
        context.registerService(QrGenerator.class, new QrGeneratorImpl());
    }
}
