package qingzhou.servlet.impl;

import qingzhou.config.Config;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.logger.Logger;
import qingzhou.servlet.ServletService;

@Module
public class Controller implements ModuleActivator {
    @Service
    static Logger logger;
    @Service
    static Config config;

    @Override
    public void start(ModuleContext context) {
        context.registerService(ServletService.class, new ServletServiceImpl());
    }
}
