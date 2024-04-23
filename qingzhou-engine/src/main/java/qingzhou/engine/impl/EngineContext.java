package qingzhou.engine.impl;

import java.io.File;
import java.io.IOException;

class EngineContext {
    private File libDir;
    private File instanceDir;
    private File temp;

    File getLibDir() {
        if (libDir == null) {
            String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/engine/qingzhou-engine.jar";
            int i = jarPath.indexOf(flag);
            String pre = jarPath.substring(0, i);
            libDir = new File(pre);
        }
        return libDir;
    }

    File getInstanceDir() {
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

    File getTemp() {
        if (temp == null) {
            temp = new File(getInstanceDir(), "temp");
        }
        return temp;
    }
}
