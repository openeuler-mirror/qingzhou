package qingzhou.engine.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import qingzhou.engine.util.FileUtil;

public class EngineContext {
    private File libDir;
    private File instanceDir;
    private File temp;
    final List<ModuleInfo> moduleInfoList = new ArrayList<>();
    String[] startArgs;

    public File getLibDir() {
        if (libDir == null) {
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/engine/qingzhou-engine.jar";
            int i = jarPath.indexOf(flag);
            String pre = jarPath.substring(0, i);
            libDir = new File(pre);
        }
        return libDir;
    }

    public File getInstanceDir() {
        if (instanceDir == null) {
            String instance = System.getProperty("qingzhou.instance");
            if (instance == null || instance.trim().isEmpty()) {
                throw new IllegalArgumentException();
            }
            this.instanceDir = new File(instance).getAbsoluteFile();
        }
        return instanceDir;
    }

    public File getTemp() {
        if (temp == null) {
            temp = FileUtil.newFile(getInstanceDir(), "temp", "engine");
        }
        return temp;
    }
}
