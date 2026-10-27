package qingzhou.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class CommandClassLoader {
    private static final String EMBED_DIR_PREFIX = "qingzhou-embed-";
    private static final long STALE_EMBED_MILLIS = 24 * 60 * 60 * 1000L;

    private static File embedDir; // 本次进程解压内嵌 jar 的临时目录，退出时删除

    static ClassLoader build(File libDir) throws Exception {
        // 命令类会直接引用 components/modules 中的类型（如 Init 的 Crypto 字段），
        // 类链接发生在实例化之前，这些 jar 必须与命令 jar 同处一个类加载器
        final String[] COMMAND_LIBS = {"command", "components", "modules"};
        // 内嵌三方 jar 的 bundle 需要额外补类路径（如 qingzhou-image 内嵌 qrcodegen）
        Set<String> embedJars = Collections.singleton("qingzhou-image.jar");
        List<URL> urls = new ArrayList<>();
        for (String lib : COMMAND_LIBS) {
            File[] jars = new File(libDir, lib).listFiles(f -> f.getName().endsWith(".jar"));
            if (jars == null) continue;
            for (File jar : jars) {
                urls.add(jar.toURI().toURL());

                if (embedJars.contains(jar.getName())) {
                    urls.addAll(loadEmbedJars(jar));
                }
            }
        }
        // 解压目录只在本次进程内有效：正常退出即删，崩溃或强杀遗留的由下次启动回收
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (embedDir != null) deleteRecursively(embedDir);
        }));
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    // 内嵌 jar 只被 OSGi 的 Bundle-ClassPath 识别，普通类路径看不到其中的类：
    // 解压到本次进程独占的临时目录后单独加入类路径
    private static List<URL> loadEmbedJars(File bundle) throws IOException {
        List<URL> urls = new ArrayList<>();
        File dir = new File(embedDir(), bundle.getName()); // 按 bundle 分目录：不同 bundle 内嵌同名 jar 不互相覆盖
        Files.createDirectories(dir.toPath());
        try (ZipFile zip = new ZipFile(bundle, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".jar")) continue;

                File outJar = Launcher.newFile(dir, entry); // 复用目录穿越校验
                Files.createDirectories(outJar.getParentFile().toPath());
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = Files.newOutputStream(outJar.toPath())) {
                    Launcher.copyStream(in, out);
                }
                urls.add(outJar.toURI().toURL());
            }
        }
        return urls;
    }

    private static File embedDir() throws IOException {
        if (embedDir == null) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            cleanStaleEmbedDirs(tmpDir);
            embedDir = Files.createTempDirectory(tmpDir.toPath(), EMBED_DIR_PREFIX).toFile();
        }
        return embedDir;
    }

    // 只回收超过一天的旧目录：并发启动的实例目录刚创建，不会被误删；
    // 即便删掉仍在运行的实例目录，已映射的 jar 在进程内仍可继续使用
    private static void cleanStaleEmbedDirs(File tmpDir) {
        File[] staleDirs = tmpDir.listFiles(f -> f.isDirectory()
                && f.getName().startsWith(EMBED_DIR_PREFIX)
                && System.currentTimeMillis() - f.lastModified() > STALE_EMBED_MILLIS);
        if (staleDirs == null) return;
        for (File dir : staleDirs) deleteRecursively(dir);
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        // Windows 上 jar 被类加载器占用时删不掉：不阻断退出，交由下次启动的过期回收兜底
        if (!file.delete()) System.err.println("failed to delete " + file);
    }
}
