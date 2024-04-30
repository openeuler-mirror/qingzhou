package qingzhou.console;

import qingzhou.engine.ModuleContext;

public interface ContextHelper {
    ThreadLocal<ContextHelper> GetInstance = new ThreadLocal<>();

    <T> T getService(Class<T> type);

    ModuleContext getModuleContext();
}
