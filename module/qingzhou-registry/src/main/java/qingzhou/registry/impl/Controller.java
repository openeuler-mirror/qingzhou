package qingzhou.registry.impl;

import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

import java.util.Timer;
import java.util.TimerTask;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Json json;

    @Service
    private Logger logger;

    @Service
    private CryptoService cryptoService;

    @Service
    private Config config;

    // 定时清理超时的自动注册实例
    private Timer timer;

    private RegistryImpl registry;

    @Override
    public void start(ModuleContext context) {
        registry = new RegistryImpl(json, cryptoService);
        context.registerService(Registry.class, registry);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    registry.timerCheck(config.getRegistry().getInstanceTimeout());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, 2000, 1000 * config.getRegistry().getInstanceInterval());
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
