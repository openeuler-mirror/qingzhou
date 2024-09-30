package qingzhou.engine.impl.core;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import qingzhou.engine.ModuleActivator;
import qingzhou.engine.impl.EngineContext;

class ModuleInfo {
    private final EngineContext engineContext;
    private final File file;

    final ModuleContextImpl moduleContext = new ModuleContextImpl(this);
    final List<ModuleActivator> moduleActivators = new ArrayList<>();

    private URLClassLoader loader;
    private boolean started;

    ModuleInfo(File file, EngineContext engineContext) {
        this.engineContext = engineContext;
        this.file = file;
    }

    EngineContext getEngineContext() {
        return engineContext;
    }

    String getName() {
        String fileName = file.getName();
        int i = fileName.indexOf("-");
        int j = fileName.indexOf(".");
        return fileName.substring(i + 1, j);
    }

    File getFile() {
        return file;
    }

    URLClassLoader getLoader() {
        return loader;
    }

    void setLoader(URLClassLoader loader) {
        this.loader = loader;
    }

    boolean isStarted() {
        return started;
    }

    void setStarted(boolean started) {
        this.started = started;
    }
}
