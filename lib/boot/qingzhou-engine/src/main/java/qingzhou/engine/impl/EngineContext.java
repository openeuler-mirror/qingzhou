package qingzhou.engine.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
            try { //兼容Windows平台中文路径或包含空白符号
                jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                System.err.println("Waring! not support UTF-8 coding. this is standard java coding");
                throw new RuntimeException("not support UTF-8 coding");
            }
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
                throw new IllegalArgumentException(); // 不要在这里设置 instance1，应该在调用端去捕捉异常并处理
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
