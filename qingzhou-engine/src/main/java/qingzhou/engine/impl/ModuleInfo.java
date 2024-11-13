package qingzhou.engine.impl;

import qingzhou.engine.ModuleActivator;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

class ModuleInfo {
    final EngineContext engineContext;

    final ModuleContextImpl moduleContext;
    final List<ModuleActivator> moduleActivators;

    private final File file;
    private URLClassLoader loader;
    private boolean started;

    ModuleInfo(File file, EngineContext engineContext) {
        this.engineContext = engineContext;
        this.file = file;

        moduleContext = new ModuleContextImpl(this);
        moduleActivators = new ArrayList<>();
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
