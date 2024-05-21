package qingzhou.engine.impl.core;

import qingzhou.engine.ModuleActivator;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

class ModuleInfo {
    private final File file;
    private URLClassLoader loader;
    private final List<ModuleActivator> moduleActivators = new ArrayList<>();
    private ModuleContextImpl moduleContext;
    private boolean started;

    ModuleInfo(File file) {
        this.file = file;
    }

    String getName() {
        return file.getName();
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

    List<ModuleActivator> getModuleActivators() {
        return moduleActivators;
    }

    ModuleContextImpl getModuleContext() {
        return moduleContext;
    }

    void setModuleContext(ModuleContextImpl moduleContext) {
        this.moduleContext = moduleContext;
    }

    boolean isStarted() {
        return started;
    }

    void setStarted(boolean started) {
        this.started = started;
    }
}
