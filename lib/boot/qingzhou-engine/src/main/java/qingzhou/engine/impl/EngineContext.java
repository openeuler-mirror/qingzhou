package qingzhou.engine.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import qingzhou.engine.util.FileUtil;

public class EngineContext {
    private final File libDir;
    private final File instanceDir;
    private File temp;
    final List<ModuleInfo> moduleInfoList = new ArrayList<>();
    String[] startArgs;

    public EngineContext(File libDir, File instanceDir) {
        this.libDir = libDir;
        this.instanceDir = instanceDir;
    }

    public File getLibDir() {
        return libDir;
    }

    public File getInstanceDir() {
        return instanceDir;
    }

    public File getTemp() {
        if (temp == null) {
            temp = FileUtil.newFile(getInstanceDir(), "temp", "engine");
        }
        return temp;
    }
}
