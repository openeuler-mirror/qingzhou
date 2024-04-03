package qingzhou.bootstrap.main;

import qingzhou.bootstrap.Utils;
import qingzhou.bootstrap.main.impl.CompositeClassLoader;
import qingzhou.bootstrap.main.impl.FrameworkContextImpl;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class Main {
    private final List<Module> moduleList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        ClassLoader api = Utils.createClassLoader(Objects.requireNonNull(new File(Utils.getLibDir(), "api").listFiles()), null);
        CompositeClassLoader multipleDependencies = new CompositeClassLoader(null);
        multipleDependencies.add(api);
        multipleDependencies.add(Module.class.getClassLoader());
        ClassLoader parentLoader = Utils.createClassLoader(Objects.requireNonNull(new File(Utils.getLibDir(), "framework").listFiles()), multipleDependencies);

        TreeMap<Integer, List<File>> moduleLevel = moduleLevel();
        for (Map.Entry<Integer, List<File>> entry : moduleLevel.entrySet()) {
            entry.getValue().forEach(file -> {
                ClassLoader classLoader = Utils.createClassLoader(new File[]{file}, parentLoader);
                startModule(classLoader);
            });
        }

        waitForStop();
    }

    private TreeMap<Integer, List<File>> moduleLevel() throws Exception {
        TreeMap<Integer, List<String>> levelProperties = new TreeMap<>();
        try (InputStream inputStream = Main.class.getResourceAsStream("/module-level.properties")) {
            Properties properties = Utils.streamToProperties(inputStream);
            properties.forEach((k, v) -> levelProperties.computeIfAbsent(Integer.parseInt((String) k), integer -> new ArrayList<>()).addAll(Arrays.asList(((String) v).split(","))));
        }
        int otherLevel = levelProperties.size() + 1;

        TreeMap<Integer, List<File>> startLevels = new TreeMap<>();

        File[] modules = new File(Utils.getLibDir(), "module").listFiles();
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
        FrameworkContext frameworkContext = new FrameworkContextImpl();
        try {
            List<Module> modules = Utils.loadServices(Module.class.getName(), classLoader);
            for (Module module : modules) {
                if (module instanceof ServiceRegister) {
                    Class<?> serviceType = ((ServiceRegister<?>) module).serviceType();
                    if (moduleList.stream().anyMatch(module1 -> (module1 instanceof ServiceRegister) && (((ServiceRegister<?>) module1).serviceType() == serviceType))) {
                        continue;
                    }
                }

                module.start(frameworkContext);
                moduleList.add(module);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForStop() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = moduleList.size() - 1; i >= 0; i--) {
                moduleList.remove(i).stop();
            }
        }));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
