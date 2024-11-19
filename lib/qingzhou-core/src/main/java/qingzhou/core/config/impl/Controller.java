package qingzhou.core.config.impl;

import qingzhou.core.config.Config;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.pattern.Process;
import qingzhou.json.Json;

import java.io.File;


public class Controller implements Process {
    private final ModuleContext context;

    public Controller(ModuleContext context) {
        this.context = context;
    }

    @Override
    public void exec() {
        context.registerService(Config.class,
                new JsonFileConfig(context.getService(Json.class),
                        new File(new File(context.getInstanceDir(), "conf"), "qingzhou.json"))
        );
    }
}
