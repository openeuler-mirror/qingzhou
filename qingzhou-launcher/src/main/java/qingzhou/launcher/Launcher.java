package qingzhou.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Launcher {
    public static void main(String[] args) throws Exception {
        // 检查是否需要 解压 升级 版本
        VersionUtil vu = new VersionUtil();
        vu.prepare();
        File libDir = vu.getLibDir();

        try (URLClassLoader cmdMainLoader = buildClassLoader(libDir)) {
            Class<?> cmdMainClass = cmdMainLoader.loadClass("qingzhou.command.Admin");
            Method cmdMainMethod = cmdMainClass.getMethod("main", String[].class);
            cmdMainMethod.invoke(null, new Object[]{args});
        }
    }

    private static URLClassLoader buildClassLoader(File libDir) throws Exception {
        List<URL> urls = new ArrayList<>();
        File[] files = new File(libDir, "command").listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().endsWith(".jar")) {
                    urls.add(file.toURI().toURL());
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]),
                null); // 不应该能依赖到 Launcher 的 jar
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static void mkdirs(File dir) {
        boolean mkdirs = dir.mkdirs();
        if (!mkdirs) {
            throw new IllegalStateException("Failed to mkdirs: " + dir.getPath());
        }
    }
}
