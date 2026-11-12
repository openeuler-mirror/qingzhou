package qingzhou.engine;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import qingzhou.engine.detector.PathDetectionScheduler;

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

        // 先安装全部 bundle，再统一启动，避免跨目录依赖解析失败
        List<Bundle> bundles = new ArrayList<>();
        for (String f : new String[]{"runtime/features", "common", "components", "AI", "modules"}) {
            bundles.addAll(installDirAndJars(new File(libDir, f)));
        }
        startBundles(bundles);

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

    static List<Bundle> installDirAndJars(File dir) throws BundleException {
        List<File> subDirs = new ArrayList<>(), subJars = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) subDirs.add(f);
            else if (f.getName().endsWith(".jar")) subJars.add(f);
        }
        List<Bundle> installed = new ArrayList<>();
        subDirs.sort(Comparator.comparing(File::getName));
        for (File d : subDirs) installed.addAll(installBundles(d.listFiles()));
        installed.addAll(installBundles(subJars.toArray(new File[0])));
        return installed;
    }

    private static List<Bundle> installBundles(File[] jars) throws BundleException {
        List<Bundle> list = new ArrayList<>();
        if (jars == null) return list;
        for (File j : jars) {
            if (isFeatureDisabled(j)) continue;
            // toURI() 做 URL 编码，避免含空格/中文路径生成非法 bundle location
            list.add(osgiFramework.getBundleContext().installBundle(j.toURI().toString()));
        }
        return list;
    }

    static void startBundles(List<Bundle> bundles) throws BundleException {
        for (Bundle b : bundles) {
            Dictionary<String, String> h = b.getHeaders();
            if (Boolean.parseBoolean(h.get("Qingzhou-Detection-Enabled")))
                new PathDetectionScheduler(b).start();
            else b.start();
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

    public static BundleContext getBundleContext() {
        return osgiFramework.getBundleContext();
    }
}
