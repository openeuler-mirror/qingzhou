package qingzhou.engine.impl;

import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;

import java.io.File;
import java.util.*;

public class ModuleInfo {
    private final String name;
    private final Set<ModuleInfo> dependencies = new HashSet<>();
    private File file;
    private ClassLoader loader;
    private final List<ModuleActivator> moduleActivators = new ArrayList<>();
    private ModuleContext moduleContext;

    public ModuleInfo(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public List<ModuleActivator> getModuleActivators() {
        return moduleActivators;
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public Set<ModuleInfo> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModuleInfo that = (ModuleInfo) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
