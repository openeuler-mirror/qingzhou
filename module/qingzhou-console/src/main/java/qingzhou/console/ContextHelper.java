package qingzhou.console;

import qingzhou.engine.ModuleContext;

public interface ContextHelper {
    ThreadLocal<ContextHelper> GetInstance = new ThreadLocal<>();

    ModuleContext getModuleContext();
}
