package qingzhou.engine.impl.core;

import qingzhou.engine.impl.EngineContext;

import java.util.List;

public interface ModuleLoaderBuilder {
    void build(List<ModuleInfo> moduleInfoList, EngineContext engineContext) throws Exception;
}
