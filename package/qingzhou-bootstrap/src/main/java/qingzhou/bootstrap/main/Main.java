package qingzhou.bootstrap.main;

import qingzhou.bootstrap.Utils;
import qingzhou.bootstrap.main.impl.FrameworkContextImpl;
import qingzhou.bootstrap.main.service.ServiceRegister;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Main {
    private static final FrameworkContext MODULE_CONTEXT = new FrameworkContextImpl();
    private static final List<ModuleLoader> MODULE_LOADER_LIST = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        TreeMap<Integer, List<File>> moduleLevel = moduleLevel();
        installModule(moduleLevel);
        waitForStop();
    }

    private static TreeMap<Integer, List<File>> moduleLevel() throws Exception {
        TreeMap<Integer, List<String>> levelProperties = new TreeMap<>();
        try (InputStream inputStream = Main.class.getResourceAsStream("/level.properties")) {
            Properties properties = Utils.streamToProperties(inputStream);
            properties.forEach((k, v) -> levelProperties.put(Integer.parseInt((String) k), new ArrayList<String>() {{
                for (String s : ((String) v).split(",")) {
                    if (!s.trim().isEmpty()) {
                        add(s.trim());
                    }
                }
            }}));
        }
        int otherLevel = levelProperties.size() + 1;

        TreeMap<Integer, List<File>> startLevels = new TreeMap<>();

        File[] modules = new File(Utils.getLibDir(), "module").listFiles();
        for (File module : Objects.requireNonNull(modules)) {
            String fileName = module.getName();
            String suffix = ".jar";
            int i = fileName.indexOf(suffix);
            if (i > 0) {
                String moduleName = fileName.substring(0, i);

                boolean match = false;
                for (Map.Entry<Integer, List<String>> entry : levelProperties.entrySet()) {
                    if (entry.getValue().contains(moduleName)) {
                        List<File> files = startLevels.computeIfAbsent(entry.getKey(), integer -> new ArrayList<>());
                        files.add(module);
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    List<File> files = startLevels.computeIfAbsent(otherLevel, integer -> new ArrayList<>());
                    files.add(module);
                }
            }
        }

        return startLevels;
    }

    private static void installModule(TreeMap<Integer, List<File>> moduleLevel) throws Exception {
        ClassLoader parent = ModuleLoader.class.getClassLoader();
        ClassLoader frameworkLoader = null;
        for (Map.Entry<Integer, List<File>> entry : moduleLevel.entrySet()) {
            for (File moduleJar : entry.getValue()) {
                if (!moduleJar.exists()) {
                    throw new IllegalStateException("module not found: " + moduleJar.getName());
                }

                // 加载当前模块
                ClassLoader currentModuleLoader = new URLClassLoader(new URL[]{moduleJar.toURI().toURL()}, parent);
                List<ModuleLoader> moduleLoaders = Utils.loadServices(ModuleLoader.class.getName(), currentModuleLoader);
                for (ModuleLoader moduleLoader : moduleLoaders) {
                    if (moduleLoader instanceof ServiceRegister) {
                        Class<?> serviceType = ((ServiceRegister<?>) moduleLoader).serviceType();
                        if (MODULE_LOADER_LIST.stream().anyMatch(moduleLoader1 -> (moduleLoader1 instanceof ServiceRegister) && (((ServiceRegister<?>) moduleLoader1).serviceType() == serviceType))) {
                            continue;
                        }
                    }

                    moduleLoader.start(MODULE_CONTEXT);
                    MODULE_LOADER_LIST.add(moduleLoader);
                }

                // 确定下个模块的父加载器
                if (moduleJar.getName().contains("qingzhou-framework")) {
                    frameworkLoader = currentModuleLoader;
                }
                if (frameworkLoader != null) {
                    parent = frameworkLoader;
                } else {
                    parent = currentModuleLoader;
                }
            }
        }
    }

    private static void waitForStop() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = MODULE_LOADER_LIST.size() - 1; i >= 0; i--) {
                MODULE_LOADER_LIST.get(i).stop(MODULE_CONTEXT);
            }
        }));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
