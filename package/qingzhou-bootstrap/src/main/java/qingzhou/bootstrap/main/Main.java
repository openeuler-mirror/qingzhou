package qingzhou.bootstrap.main;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import qingzhou.bootstrap.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) throws Exception {
        TreeMap<Integer, Set<File>> moduleLevel = moduleLevel();

        Map<String, String> configuration = new HashMap<>();
        File bundleCache = Utils.getCache(Utils.getDomain(), "bundle");
        if (bundleCache.isDirectory()) {
            Utils.cleanDirectory(bundleCache);
        }
        configuration.put(Constants.FRAMEWORK_STORAGE, bundleCache.getCanonicalPath());
        configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, String.valueOf(moduleLevel.size()));

        List<FrameworkFactory> frameworkFactories = Utils.loadServices(FrameworkFactory.class.getName(), Main.class.getClassLoader());
        FrameworkFactory frameworkFactory = frameworkFactories.get(0);
        Framework framework = frameworkFactory.newFramework(configuration);

        framework.start();
        installBundle(framework, moduleLevel);
        waitForStop(framework);
    }

    private static TreeMap<Integer, Set<File>> moduleLevel() throws Exception {
        TreeMap<Integer, Set<String>> levelProperties = new TreeMap<>();
        try (InputStream inputStream = Main.class.getResourceAsStream("/level.properties")) {
            Properties properties = Utils.streamToProperties(inputStream);
            properties.forEach((k, v) -> levelProperties.put(Integer.parseInt((String) k), new HashSet<String>() {{
                for (String s : ((String) v).split(",")) {
                    if (!s.trim().isEmpty()) {
                        add(s.trim());
                    }
                }
            }}));
        }
        int otherLevel = levelProperties.size() + 1;

        TreeMap<Integer, Set<File>> startLevels = new TreeMap<>();

        String[] searchDir = {"module", "service"};
        for (String dir : searchDir) {
            File[] modules = new File(Utils.getLibDir(), dir).listFiles();
            for (File module : Objects.requireNonNull(modules)) {
                String fileName = module.getName();
                String suffix = ".jar";
                int i = fileName.indexOf(suffix);
                if (i > 0) {
                    String moduleName = fileName.substring(0, i);

                    boolean match = false;
                    for (Map.Entry<Integer, Set<String>> entry : levelProperties.entrySet()) {
                        if (entry.getValue().contains(moduleName)) {
                            Set<File> files = startLevels.computeIfAbsent(entry.getKey(), integer -> new HashSet<>());
                            files.add(module);
                            match = true;
                            break;
                        }
                    }

                    if (!match) {
                        Set<File> files = startLevels.computeIfAbsent(otherLevel, integer -> new HashSet<>());
                        files.add(module);
                    }
                }
            }
        }

        return startLevels;
    }

    private static void installBundle(Framework framework, TreeMap<Integer, Set<File>> moduleLevel) {
        for (Map.Entry<Integer, Set<File>> entry : moduleLevel.entrySet()) {
            entry.getValue().forEach(moduleJar -> {
                if (moduleJar.exists()) {
                    installBundleFile(framework, moduleJar);
                } else {
                    System.err.println("module not found: " + moduleJar.getName());
                }
            });
        }

        File[] apps = new File(Utils.getDomain(), "apps").listFiles();
        if (apps != null) {
            for (File app : apps) {
                installBundleFile(framework, app);
            }
        }
    }

    private static void installBundleFile(Framework framework, File moduleJar) {
        BundleContext bundleContext = framework.getBundleContext();
        try {
            Bundle bundle = bundleContext.installBundle(moduleJar.toURI().toString());
            bundle.start();
        } catch (BundleException e) {
            System.err.println("Failed to load module " + moduleJar.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void waitForStop(Framework framework) {
        Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
            @Override
            public void run() {
                try {
                    framework.stop();
                    framework.waitForStop(0L);
                } catch (Exception ex) {
                    System.err.println("Error stopping framework: " + ex);
                }
            }
        });
    }
}
