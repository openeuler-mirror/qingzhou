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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class AppDeployer {
    private final String instanceDir;
    private final String libDir;

    private URLClassLoader tempClassLoader;
    private Method bundleConverterMethod;
    private Object bundleConverterInstance;

    AppDeployer(String instanceDir, String libDir) {
        this.instanceDir = instanceDir;
        this.libDir = libDir;
    }

    void deployApps() throws Exception {
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

        QingzhouMain.startDirAndJars(appCacheDir);
        tempClassLoader.close();
    }

    private void convertBundleJar(File sourceJar, File targetJar, String libDir) throws Exception {
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

            tempClassLoader = new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
            Class<?> loadedClass = tempClassLoader.loadClass("qingzhou.bundle.converter.BundleConverter");
            bundleConverterMethod = loadedClass.getMethod("build", File.class, File.class, String.class);
            bundleConverterInstance = loadedClass.newInstance();
        }
        bundleConverterMethod.invoke(bundleConverterInstance, sourceJar, targetJar, libDir);
    }

    private void addFiles(List<URL> urls, String... dirs) throws Exception {
        File[] listFiles = Paths.get(libDir, dirs).toFile().listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                urls.add(file.toURI().toURL());
            }
        }
    }

    private void unZipToDir(File srcFile, File unZipDir) throws IOException {
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

    private void deleteFile(File file) {
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
