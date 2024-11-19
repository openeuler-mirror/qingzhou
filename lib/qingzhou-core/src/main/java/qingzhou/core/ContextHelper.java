package qingzhou.core;

import qingzhou.engine.ModuleContext;

public interface ContextHelper {
    ThreadLocal<ContextHelper> GET_INSTANCE = new ThreadLocal<>();

    ModuleContext getModuleContext();
}
