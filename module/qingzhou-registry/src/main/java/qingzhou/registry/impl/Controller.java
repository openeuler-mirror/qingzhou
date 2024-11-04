package qingzhou.registry.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

import java.util.Map;
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

    // 定时清理超时的自动注册实例
    private Timer timer;

    private RegistryImpl registry;

    @Override
    public void start(ModuleContext context) {
        registry = new RegistryImpl(json, cryptoService);
        context.registerService(Registry.class, registry);

        Map<String, String> config = context.getConfig();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    registry.timerCheck(Long.parseLong(config.get("checkTimeout")));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, 2000, 1000 * Long.parseLong(config.get("checkInterval")));
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
