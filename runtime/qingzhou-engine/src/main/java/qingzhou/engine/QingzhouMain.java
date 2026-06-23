package qingzhou.engine;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class QingzhouMain {
    private static String instanceDir;
    private static Framework osgiFramework;
    private static ScheduledExecutorService scheduledExecutor;

    public static void main(String[] args) throws Exception {
        // 解码 URL 编码的路径
        String jarPath = Paths.get(
                QingzhouMain.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).toString();

        String libDir = new File(jarPath).getParentFile().getParentFile().getAbsolutePath();
        instanceDir = System.getProperty("qingzhou.instance");

        // 获取 FrameworkFactory
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        if (factory == null) throw new RuntimeException("No OSGi framework found");
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

            Bundle bundle = osgiFramework.getBundleContext().installBundle("file:" + bundleJar.getAbsolutePath());
            bundleList.add(bundle);
        }
        for (Bundle bundle : bundleList) {
            Dictionary<String, String> bundleHeaders = bundle.getHeaders();
            boolean detectionEnabled = Boolean.parseBoolean(bundleHeaders.get("Qingzhou-Detection-Enabled"));
            if (detectionEnabled) {
                startWithDetection(bundle);
            } else {
                bundle.start();
            }
        }
    }

    private static void startWithDetection(Bundle bundle) {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try { // 实际检测逻辑
                PathDetector detector = new PathDetector(bundle.getHeaders());
                String detected = detector.detect();
                if (detected != null) {
                    System.setProperty("Qingzhou-Detected-Path", detected);
                    bundle.start();
                } else {
                    bundle.stop();
                }
            } catch (Throwable e) {
                e.printStackTrace(); // 捕获并打印，保证任务不被取消
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private static boolean isFeatureDisabled(File file) {
        String featuresDisabled = System.getProperty("qingzhou.features.disabled");
        if (featuresDisabled != null) {
            // 确定要检查的文件名
            String name = file.getName();
            if (name.endsWith(".jar")) {
                name = name.substring(0, name.length() - ".jar".length());
            }

            // 开始检查
            String[] checkList = featuresDisabled.split(",");
            for (String check : checkList) {
                if (check.trim().equals(name)) return true;
            }
        }

        return false;
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
