package qingzhou.engine.impl;

import qingzhou.engine.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EngineContext {
    private File libDir;
    private File instanceDir;
    private File temp;

    public File getLibDir() {
        if (libDir == null) {
            String jarPath = java.net.URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath(), StandardCharsets.UTF_8);
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
                throw new IllegalArgumentException();// 不要在这里设置 instance1，应该在调用端去捕捉异常并处理
            }
            try {
                this.instanceDir = new File(instance).getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException();// 不要在这里设置 instance1，应该在调用端去捕捉异常并处理
            }
        }
        return instanceDir;
    }

    public File getTemp() {
        if (temp == null) {
            temp = Utils.newFile(getInstanceDir(), "temp", "engine");
        }
        return temp;
    }
}
