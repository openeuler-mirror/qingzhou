package qingzhou.core.registry.impl;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import qingzhou.core.registry.Registry;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.pattern.Process;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

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

        Map<String, String> deployer = (Map<String, String>) ((Map<String, Object>) context.getConfig()).get("deployer");
        if (Boolean.parseBoolean(deployer.get("staticMode"))) {
            // 静态模式下，不能进行应用和实例的“卸载”和“添加”，注册服务也会关闭
            return;
        }

        Map<String, String> config = (Map<String, String>) ((Map<String, Object>) context.getConfig()).get("registry");
        if (config == null || !Boolean.parseBoolean(config.get("enabled"))) return;

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
