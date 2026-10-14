package qingzhou.engine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
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

        File jarParent = new File(jarPath).getParentFile();
        if (jarParent == null || jarParent.getParentFile() == null) {
            throw new IllegalStateException("jarPath is not a valid path" + jarPath);
        }
        String libDir = jarParent.getParentFile().getAbsolutePath();
        instanceDir = System.getProperty("qingzhou.instance");
        checkRequiredConfigs();

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
            if (scheduledExecutor != null) {
                scheduledExecutor.shutdownNow();
            }
            try {
                osgiFramework.stop();
                osgiFramework.waitForStop(0);
            } catch (Exception ignored) {
            }
        }, QingzhouMain.class.getName() + "-ShutdownHook"));
    }

    // 必需配置清单，缺失（文件缺失、键不存在、值为空白）即禁止启动，引导执行 init 脚本完成系统初始化
    private static final String[] REQUIRED_CONFIGS = {"qingzhou-auth.password"};

    private static void checkRequiredConfigs() {
        File propsFile = instanceDir == null ? null
                : Paths.get(instanceDir, "conf", "qingzhou.properties").toFile();
        Properties props = new Properties();
        if (propsFile != null && propsFile.isFile()) {
            try (Reader reader = Files.newBufferedReader(propsFile.toPath())) {
                props.load(reader);
            } catch (IOException ignored) { // 读取失败按未配置处理，与缺失同走初始化引导
            }
        }
        for (String key : REQUIRED_CONFIGS) {
            String value = props.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                String instanceName = instanceDir == null ? "<instance>" : new File(instanceDir).getName();
                System.err.println(key + " is not configured in "
                        + "instances/" + instanceName + "/conf/qingzhou.properties"
                        + ", run bin/init.sh to initialize the instance");
                System.exit(1);
            }
        }
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
                startWithDetection(bundle);
            } else {
                bundle.start();
            }
        }
    }

    private static void startWithDetection(Bundle bundle) {
        if (scheduledExecutor == null) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "qz-path-detector");
                thread.setDaemon(true);
                return thread;
            });
        }
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try { // 实际检测逻辑
                PathDetector detector = new PathDetector(bundle.getHeaders());
                String detected = detector.detect();
                if (detected != null) {
                    // 按 bundle 符号名分键传递，避免多个探测型 bundle 互相覆盖，
                    // 也避免"设置后即清除"与 Activator 异步读取之间的竞态；属性值随检测覆盖更新，无需清除
                    String dataFlag = "Qingzhou-Detected-Path." + bundle.getSymbolicName();
                    System.setProperty(dataFlag, detected);
                    bundle.start();
                } else {
                    bundle.stop();
                }
            } catch (Throwable e) {
                e.printStackTrace(System.err); // 捕获并打印，保证任务不被取消
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private static volatile Set<String> disabledFeatures; // 启动期一次性解析，避免逐 jar 重复 split

    private static boolean isFeatureDisabled(File file) {
        Set<String> features = disabledFeatures;
        if (features == null) {
            features = new HashSet<>();
            String featuresDisabled = System.getProperty("qingzhou.features.disabled");
            if (featuresDisabled != null) {
                for (String check : featuresDisabled.split(",")) {
                    if (!check.trim().isEmpty()) features.add(check.trim());
                }
            }
            disabledFeatures = features;
        }

        String name = file.getName();
        if (name.endsWith(".jar")) {
            name = name.substring(0, name.length() - ".jar".length());
        }
        return features.contains(name);
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
