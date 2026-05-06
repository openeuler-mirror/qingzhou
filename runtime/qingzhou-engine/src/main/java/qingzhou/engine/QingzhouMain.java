package qingzhou.engine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class QingzhouMain {
    private static String libDir;
    private static String instanceDir;
    private static Framework osgiFramework;
    private static URLClassLoader tempClassLoader;

    public static void main(String[] args) throws Exception {
        // 解码 URL 编码的路径
        String jarPath = Paths.get(
                QingzhouMain.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).toString();

        libDir = new File(jarPath).getParentFile().getParentFile().getAbsolutePath();
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
        deployApps();

        // 清理资源
        if (tempClassLoader != null) {
            tempClassLoader.close();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                osgiFramework.stop();
                osgiFramework.waitForStop(0);
            } catch (Exception ignored) {
            }
        }, QingzhouMain.class.getName() + "-ShutdownHook"));
    }

    private static void deployApps() throws Exception {
        File appCacheDir = Paths.get(instanceDir, "temp", "app-bundle-cache").toFile();
        if (appCacheDir.exists()) {
            deleteFile(appCacheDir);
        }
        appCacheDir.mkdirs();

        File[] listFiles = new File(instanceDir, "apps").listFiles();
        if (listFiles != null) {
            for (File appFile : listFiles) {
                if (appFile.getName().endsWith(".jar")) {
                    File appCache = new File(appCacheDir, appFile.getName());
                    convertBundleJar(appFile, appCache, libDir);
                }
            }
        }

        startDirAndJars(appCacheDir);
    }

    private static void startDirAndJars(File bundleDir) throws BundleException {
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

        subDirs.sort(Comparator.comparing(File::getName)); // logger 排在最前面
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
            bundle.start();
        }
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
        String pkgExtra = System.getProperty("qingzhou.osgi.packages.extra");
        if (pkgExtra != null && !pkgExtra.trim().isEmpty()) {
            config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, pkgExtra.trim());
        }
        return config;
    }

    private static Method bundleConverterMethod;
    private static Object bundleConverterInstance;

    private static void convertBundleJar(File sourceJar, File targetJar, String libDir) throws Exception {
        if (bundleConverterMethod == null || bundleConverterInstance == null) {
            File appCacheDir = Paths.get(instanceDir, "temp", "bundle-converter-classpath-cache").toFile();
            if (appCacheDir.exists()) {
                deleteFile(appCacheDir);
            }
            if (!appCacheDir.mkdirs()) throw new IllegalStateException(appCacheDir.getPath());

            List<URL> urls = new ArrayList<>();
            addFiles(urls, "runtime", "bundle-converter");
            addFiles(urls, "runtime", "app-driver");

            urls.add(Paths.get(libDir, "modules", "qingzhou-api.jar").toUri().toURL());
            urls.add(Paths.get(libDir, "modules", "qingzhou-dto.jar").toUri().toURL());

            Path jsonBundlePath = Paths.get(libDir, "components", "qingzhou-json.jar");
            urls.add(jsonBundlePath.toUri().toURL());
            unZipToDir(jsonBundlePath.toFile(), appCacheDir); // 添加 bundle-converter 依赖的三方库
            File[] listFiles = appCacheDir.listFiles();
            for (File temp : Objects.requireNonNull(listFiles)) {
                if (temp.getName().equals("OSGI-INF")) {
                    File[] embeddedJars = Paths.get(appCacheDir.getAbsolutePath(), "OSGI-INF", "lib").toFile().listFiles();
                    for (File embeddedJar : Objects.requireNonNull(embeddedJars)) {
                        urls.add(embeddedJar.toURI().toURL());
                    }
                } else {
                    deleteFile(temp);
                }
            }

            tempClassLoader = new URLClassLoader(urls.toArray(new URL[0]), osgiFramework.getClass().getClassLoader());
            Class<?> loadedClass = tempClassLoader.loadClass("qingzhou.bundle.converter.BundleConverter");
            bundleConverterMethod = loadedClass.getMethod("build", File.class, File.class, String.class);
            bundleConverterInstance = loadedClass.newInstance();
        }
        bundleConverterMethod.invoke(bundleConverterInstance, sourceJar, targetJar, libDir);
    }

    private static void addFiles(List<URL> urls, String... dirs) throws Exception {
        File[] listFiles = Paths.get(libDir, dirs).toFile().listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                urls.add(file.toURI().toURL());
            }
        }
    }

    private static void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = new File(unZipDir, entry.getName());
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        InputStream inputStream = zip.getInputStream(entry);
                        byte[] buffer = new byte[1024 * 4];
                        int n;
                        while (-1 != (n = inputStream.read(buffer))) {
                            out.write(buffer, 0, n);
                        }
                    }
                }
            }
        }
    }

    private static void deleteFile(File file) {
        if (!file.exists()) return;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFile(child);
                }
            }
        }
        file.delete();
    }
}
