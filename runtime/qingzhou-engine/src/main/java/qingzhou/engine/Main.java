package qingzhou.engine;

import qingzhou.engine.impl.CompositeClassLoader;
import qingzhou.engine.impl.ModuleContextImpl;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Main {
    private final List<Module> moduleList = new ArrayList<>();
    private final ModuleContext moduleContext = new ModuleContextImpl();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        ClassLoader api = createClassLoader(Objects.requireNonNull(new File(moduleContext.getLibDir(), "api").listFiles()), null);
        CompositeClassLoader multipleDependencies = new CompositeClassLoader(null);
        multipleDependencies.add(api);
        multipleDependencies.add(Module.class.getClassLoader());
        ClassLoader parentLoader = createClassLoader(Objects.requireNonNull(new File(moduleContext.getLibDir(), "framework").listFiles()), multipleDependencies);

        TreeMap<Integer, List<File>> moduleLevel = moduleLevel();
        for (Map.Entry<Integer, List<File>> entry : moduleLevel.entrySet()) {
            entry.getValue().forEach(file -> {
                ClassLoader classLoader = createClassLoader(new File[]{file}, parentLoader);
                startModule(classLoader);
            });
        }

        waitForStop();
    }

    private static ClassLoader createClassLoader(File[] jarFiles, ClassLoader parent) {
        URL[] urls = new URL[jarFiles.length];
        try {
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return new URLClassLoader(urls, parent);
    }

    private TreeMap<Integer, List<File>> moduleLevel() throws Exception {
        TreeMap<Integer, List<String>> levelProperties = new TreeMap<>();
        try (InputStream inputStream = Main.class.getResourceAsStream("/module-level.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.forEach((k, v) -> levelProperties.computeIfAbsent(Integer.parseInt((String) k), integer -> new ArrayList<>()).addAll(Arrays.asList(((String) v).split(","))));
        }
        int otherLevel = levelProperties.size() + 1;

        TreeMap<Integer, List<File>> startLevels = new TreeMap<>();

        File[] modules = new File(moduleContext.getLibDir(), "module").listFiles();
        for (File module : Objects.requireNonNull(modules)) {
            String fileName = module.getName();
            int i = fileName.indexOf(".jar");
            if (i <= 0) continue;

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
