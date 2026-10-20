package qingzhou.engine;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class QingzhouMain {
    private static String instanceDir;
    private static Framework osgiFramework;

    public static void main(String[] args) throws Exception {
        // 解码 URL 编码的路径
        String jarPath = Paths.get(
                QingzhouMain.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).toString();

        File jarParent = new File(jarPath).getParentFile();
        if (jarParent == null || jarParent.getParentFile() == null) {
            throw new IllegalStateException("jarPath is not a valid path: " + jarPath);
        }
        String libDir = jarParent.getParentFile().getAbsolutePath();
        instanceDir = System.getProperty("qingzhou.instance");
        EnvCheck.checkRequiredConfigs(instanceDir);

        // 获取 FrameworkFactory
        Iterator<FrameworkFactory> factories = ServiceLoader.load(FrameworkFactory.class).iterator();
        if (!factories.hasNext()) throw new IllegalStateException("No OSGi framework found");
        FrameworkFactory factory = factories.next();
        osgiFramework = factory.newFramework(osgiConfig());
        osgiFramework.init();
        osgiFramework.start();

        // 启动 OSGI 基础框架
        startDirAndJars(Paths.get(libDir, "runtime", "features").toFile());

        // 启动基础模块
        startDirAndJars(new File(libDir, "components"));

        // 启动业务模块
        startDirAndJars(new File(libDir, "modules"));

        // 驱动应用
        new AppDeployer(instanceDir, libDir).deployApps();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PathDetectionScheduler.shutdown();
            try {
                osgiFramework.stop();
                osgiFramework.waitForStop(0);
            } catch (Exception ignored) {
            }
        }, QingzhouMain.class.getName() + "-ShutdownHook"));
    }

    static void startDirAndJars(File bundleDir) throws BundleException {
        List<File> subDirs = new ArrayList<>();
        List<File> subJars = new ArrayList<>();

        File[] listFiles = bundleDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    subDirs.add(file);
                } else if (file.getName().endsWith(".jar")) {
                    subJars.add(file);
                }
            }
        }

        subDirs.sort(Comparator.comparing(File::getName));
        subDirs.forEach(dir -> {
            try {
                startBundles(dir.listFiles());
            } catch (BundleException e) {
                throw new RuntimeException(e);
            }
        });
        startBundles(subJars.toArray(new File[0]));
    }

    private static void startBundles(File[] bundleJars) throws BundleException {
        if (bundleJars == null || bundleJars.length == 0) return;

        List<Bundle> bundleList = new ArrayList<>();
        for (File bundleJar : bundleJars) {
            if (isFeatureDisabled(bundleJar)) continue;

            // toURI() 对路径做 URL 编码，含空格/中文的安装目录不再产生非法的 bundle location
            Bundle bundle = osgiFramework.getBundleContext().installBundle(bundleJar.toURI().toString());
            bundleList.add(bundle);
        }
        for (Bundle bundle : bundleList) {
            Dictionary<String, String> bundleHeaders = bundle.getHeaders();
            boolean detectionEnabled = Boolean.parseBoolean(bundleHeaders.get("Qingzhou-Detection-Enabled"));
            if (detectionEnabled) {
                PathDetectionScheduler.startWithDetection(bundle);
            } else {
                bundle.start();
            }
        }
    }

    private static boolean isFeatureDisabled(File file) {
        String name = file.getName();
        if (name.endsWith(".jar")) {
            name = name.substring(0, name.length() - ".jar".length());
        }
        return disabledFeatures.contains(name);
    }

    // 启动期一次性解析，避免逐 jar 重复 split；构建期即确定，无需 volatile / 懒加载
    private static final Set<String> disabledFeatures = loadDisabledFeatures();

    private static Set<String> loadDisabledFeatures() {
        Set<String> features = new HashSet<>();
        String featuresDisabled = System.getProperty("qingzhou.features.disabled");
        if (featuresDisabled != null) {
            for (String check : featuresDisabled.split(",")) {
                if (!check.trim().isEmpty()) features.add(check.trim());
            }
        }
        return features;
    }

    private static Map<String, String> osgiConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE, Paths.get(instanceDir, "temp", "osgi-cache").toFile().getAbsolutePath());
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        String pkgExtra = System.getProperty("qingzhou.osgi.packages.import.system");
        if (pkgExtra != null && !pkgExtra.trim().isEmpty()) {
            config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, pkgExtra.trim());
        }
        return config;
    }
}
