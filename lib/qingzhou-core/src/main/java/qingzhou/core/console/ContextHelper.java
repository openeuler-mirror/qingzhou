package qingzhou.core.console;

import qingzhou.engine.ModuleContext;

public interface ContextHelper {
    ThreadLocal<ContextHelper> GET_INSTANCE = new ThreadLocal<>();

    ModuleContext getModuleContext();
}
