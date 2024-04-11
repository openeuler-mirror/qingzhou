package qingzhou.engine.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.ServiceRegister;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    private final List<Module> moduleList = new ArrayList<>();
    private final ModuleContext moduleContext = new ModuleContextImpl();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        CompositeClassLoader rootLoader = new CompositeClassLoader(null);
        File apiJarFile = new File(moduleContext.getLibDir(), "qingzhou-api.jar");
        rootLoader.add(new URLClassLoader(new URL[]{apiJarFile.toURI().toURL()}, rootLoader));
        rootLoader.add(Module.class.getClassLoader());

        ClassLoader parentLoader = rootLoader;
        List<List<File>> startLevel = startLevel();
        for (List<File> sameLevelFiles : startLevel) {
            CompositeClassLoader levelLoader = new CompositeClassLoader(null);
            for (File moduleFile : sameLevelFiles) {
                ClassLoader[] loaders = splitApiLoader(moduleFile, parentLoader);
                ClassLoader moduleApiLoader = loaders[0];
                startModule(loaders[1]);

                levelLoader.add(moduleApiLoader);
            }

            parentLoader = levelLoader;
        }

        waitForStop();
    }

    private ClassLoader[] splitApiLoader(File moduleFile, ClassLoader parentLoader) {
        String moduleFileName = moduleFile.getName();
        int i = moduleFileName.lastIndexOf(".");
        String moduleName = moduleFileName.substring(0, i);
        moduleName = moduleName.replace("-", ".");
        ClassLoader apiLoader = new SplitLoader(parentLoader);
        ClassLoader implLoader = new SplitLoader(apiLoader);
        return new ClassLoader[]{apiLoader, implLoader};
    }

    private class SplitLoader extends ClassLoader {
        protected SplitLoader(ClassLoader parent) {
            super(parent);
        }
    }

    private List<List<File>> startLevel() throws Exception {
        List<List<File>> startLevels = new ArrayList<>();

        try (InputStream inputStream = Main.class.getResourceAsStream("/start-level.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8));
            File moduleDir = new File(moduleContext.getLibDir(), "module");
            for (String line; (line = reader.readLine()) != null; ) {
                line = line.trim();
                if (line.isEmpty()) continue;

                List<File> sameLevelFiles = new ArrayList<>();
                for (String module : line.split(",")) {
                    File moduleJar = new File(moduleDir, module.trim() + ".jar");
                    if (!moduleJar.isFile()) continue;

                    sameLevelFiles.add(moduleJar);
                }
                if (!sameLevelFiles.isEmpty()) {
                    startLevels.add(sameLevelFiles);
                }
            }
        }

        Collections.reverse(startLevels);
        return startLevels;
    }

    private void startModule(ClassLoader classLoader) {
        try {
            for (Module module : ServiceLoader.load(Module.class, classLoader)) {
                if (module instanceof ServiceRegister) {
                    Class<?> serviceType = ((ServiceRegister<?>) module).serviceType();
                    if (moduleList.stream().anyMatch(module1 -> (module1 instanceof ServiceRegister) && (((ServiceRegister<?>) module1).serviceType() == serviceType))) {
                        continue;
                    }
                }

                module.start(moduleContext);
                moduleList.add(module);

                System.out.println("The module is loaded: " + module.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForStop() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = moduleList.size() - 1; i >= 0; i--) {
                Module module = moduleList.remove(i);
                module.stop();

                System.out.println("The module has been unloaded: " + module.getClass().getName());
            }
        }));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
