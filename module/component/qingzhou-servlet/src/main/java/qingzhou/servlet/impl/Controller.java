package qingzhou.servlet.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.logger.Logger;
import qingzhou.servlet.ServletService;

@Module
public class Controller implements ModuleActivator {
    static Logger logger;

    @Override
    public void start(ModuleContext context) {
        context.registerService(ServletService.class, new ServletServiceImpl());
    }
}
