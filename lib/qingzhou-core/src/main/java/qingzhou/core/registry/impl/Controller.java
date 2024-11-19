package qingzhou.core.registry.impl;

import qingzhou.core.registry.Registry;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.pattern.Process;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Controller implements Process {
    private final ModuleContext context;

    // 定时清理超时的自动注册实例
    private Timer timer;

    private RegistryImpl registry;

    public Controller(ModuleContext context) {
        this.context = context;
    }

    @Override
    public void exec() {
        registry = new RegistryImpl(context.getService(Json.class), context.getService(CryptoService.class));
        context.registerService(Registry.class, registry);

        Map<String, String> config = (Map<String, String>) ((Map<String, Object>) context.getConfig()).get("registry");
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    registry.timerCheck(Long.parseLong(config.get("checkTimeout")));
                } catch (Exception e) {
                    context.getService(Logger.class).error(e.getMessage(), e);
                }
            }
        }, 2000, 1000 * Long.parseLong(config.get("checkInterval")));
    }

    @Override
    public void undo() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
