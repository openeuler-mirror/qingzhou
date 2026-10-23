package qingzhou.launcher;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Launcher {
    private static final String ZIP_FORMAT = ".zip";
    // 命令类会直接引用 components/modules 中的类型（如 Init 的 Crypto 字段），
    // 类链接发生在实例化之前，这些 jar 必须与命令 jar 同处一个类加载器
    private static final String[] COMMAND_LIBS = {"command", "components", "modules"};

    private static File homeFile;

    public static void main(String[] args) throws Exception {
        initHomeFile(); // 确定主目录
        checkLatestZipVersion(); // 检查是否需要 解压 升级 版本
        File libDir = getLatestLibDir(); // 获得最新版本

        // 命令类加载器与进程同生命周期：doCommand 可能拉起常驻平台，不得提前关闭
        ClassLoader cmdMainLoader = buildCommandClassLoader(libDir);
        Class<?> cmdMainClass = cmdMainLoader.loadClass("qingzhou.command.Command");
        Constructor<?> constructor = cmdMainClass.getConstructor(File.class);
        Object newInstance = constructor.newInstance(libDir);
        Method doCommandMethod = cmdMainClass.getMethod("doCommand", String[].class);
        doCommandMethod.invoke(newInstance, new Object[]{args});
    }

    static void initHomeFile() throws URISyntaxException {
        // 解码 URL 编码的路径
        String jarPath = Paths.get(
                Launcher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).toString();
        File jarParent = new File(jarPath).getParentFile();
        if (jarParent == null || jarParent.getParentFile() == null) {
            throw new IllegalStateException("jarPath is not a valid path: " + jarPath);
        }
        homeFile = jarParent.getParentFile();
    }

    static void checkLatestZipVersion() {
        String zipLatestVer = latestVersion(f -> !f.isDirectory()
                && f.getName().startsWith("version") && f.getName().endsWith(ZIP_FORMAT));

        File verFile = Paths.get(homeFile.getAbsolutePath(), "lib", "version" + zipLatestVer + ZIP_FORMAT).toFile();
        if (!verFile.exists()) return;

        File libDir = Paths.get(homeFile.getAbsolutePath(), "lib", "version" + zipLatestVer).toFile();
        if (libDir.exists()) return;

        try {
            unZipToDir(verFile, libDir);
        } catch (IOException ex) {
            System.err.println("Failed to unzip " + verFile + ": " + ex
                    + System.lineSeparator()
                    + "Please manually unzip it to " + libDir
                    + ", or delete this directory and restart to trigger re-unzip.");
            System.exit(1);
        }
    }

    static File getLatestLibDir() {
        String version = latestVersion(f -> f.isDirectory() && f.getName().startsWith("version"));
        if (version.isEmpty()) {
            System.err.println("qingzhou library not found");
            System.exit(1);
        }
        return Paths.get(homeFile.getAbsolutePath(), "lib", "version" + version).toFile();
    }

    // 扫描 lib 目录下 version* 形式的文件或目录，返回版本号最大的那个；无匹配时返回空串
    private static String latestVersion(FileFilter filter) {
        String latest = "";
        File[] listFiles = new File(homeFile, "lib").listFiles(filter);
        if (listFiles != null) {
            for (File file : listFiles) {
                String ver = file.getName().substring("version".length());
                if (ver.endsWith(ZIP_FORMAT)) {
                    ver = ver.substring(0, ver.length() - ZIP_FORMAT.length());
                }
                if (isLaterVersion(ver, latest)) {
                    latest = ver;
                }
            }
        }
        return latest;
    }

    private static ClassLoader buildCommandClassLoader(File libDir) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String lib : COMMAND_LIBS) {
            File[] jars = new File(libDir, lib).listFiles(f -> f.getName().endsWith(".jar"));
            if (jars == null) continue;
            for (File jar : jars) {
                urls.add(jar.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    // 引导模块保持零依赖（先于平台类库加载，不能引用 app-driver 的 FileUtil），故本地保留解压实现
    private static void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = newFile(unZipDir, entry);
                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        boolean mkdirs = targetFile.mkdirs();
                        if (!mkdirs) {
                            throw new IOException("failed to create directory " + targetFile);
                        }
                    }
                } else {
                    File parentFile = targetFile.getParentFile();
                    if (!parentFile.exists()) {
                        boolean mkdirs = parentFile.mkdirs();
                        if (!mkdirs) {
                            throw new IOException("failed to create directory " + targetFile.getParentFile());
                        }
                    }
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath());
                         InputStream in = zip.getInputStream(entry)) {
                        copyStream(in, out);
                    } catch (IOException ex) {
                        // 只容错 IO 异常（如部分环境下 docs 中文名解压失败）；
                        // Error 等严重问题不吞，避免掩盖包损坏的真实原因
                        if (parentFile.getName().equals("docs")) {
                            System.err.println("Failed to extract docs file, please manually unzip it if needed: " + entry.getName() + ", " + ex.getMessage());
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private static File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
            throw new IOException("Entry is outside of target dir:" + entry.getName()); // 防止压缩包解压目录穿越风险
        }
        return destFile;
    }

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    private static boolean isLaterVersion(String v1, String v2) {
        v1 = v1.trim();
        v2 = v2.trim();
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");

        int len1 = arr1.length;
        int len2 = arr2.length;
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            String c1 = arr1[k];
            String c2 = arr2[k];
            if (!c1.equals(c2)) {
                try {
                    return Integer.parseInt(c1) > Integer.parseInt(c2);
                } catch (NumberFormatException e) {
                    return c1.compareTo(c2) > 0;
                }
            }
            k++;
        }
        return len1 > len2;
    }
}
