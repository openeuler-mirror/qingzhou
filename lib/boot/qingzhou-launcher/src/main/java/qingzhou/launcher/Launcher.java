package qingzhou.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
    static final String VERSION_DIR_NAME = "version"; // 不能改变
    private static File homeFile;
    private static int failedDocsCount = 0;//处理 docs 文件夹文件时的失败次数

    public static void main(String[] args) throws Exception {
        initHomeFile(); // 确定主目录
        prepareVersion(); // 检查是否需要 解压 升级 版本
        File libDir = getLibDir(); // 获得最新版本
        System.setProperty("qingzhou.lib", libDir.getAbsolutePath());

        ClassLoader cmdMainLoader = buildAdminClassLoader(libDir);
        Class<?> cmdMainClass = cmdMainLoader.loadClass("qingzhou.command.Admin");
        Method cmdMainMethod = cmdMainClass.getMethod("main", String[].class);
        cmdMainMethod.invoke(null, new Object[]{args});
    }

    static void initHomeFile() {
        String home = System.getProperty("qingzhou.home");
        if (home == null || home.trim().isEmpty()) {
            home = System.getenv("qingzhou_home");
        }
        if (home == null || home.trim().isEmpty()) {
            String jarPath = Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/bin/qingzhou-launcher.jar";
            int i = jarPath.lastIndexOf(flag);
            String pre = jarPath.substring(0, i);
            home = new File(pre).getPath();
        }
        homeFile = new File(home);
        if (homeFile.exists()) {
            System.setProperty("qingzhou.home", homeFile.getAbsolutePath());
        } else {
            throw new IllegalStateException("qingzhou home not found");
        }
    }

    static void prepareVersion() throws Exception { // 保持一致：BaseUtil.getLatestLibDir
        String version = "";
        String format = ".zip";
        File[] listFiles = new File(homeFile, "lib").listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                String fileName = file.getName();
                if (fileName.startsWith(VERSION_DIR_NAME) && fileName.endsWith(format)) {
                    String ver = fileName.substring(VERSION_DIR_NAME.length(), fileName.length() - format.length());
                    if (isLaterVersion(ver, version)) {
                        version = ver;
                    }
                }
            }
        }

        File verFile = Paths.get(homeFile.getAbsolutePath(), "lib", VERSION_DIR_NAME + version + format).toFile();
        if (verFile.exists()) {
            File libDir = Paths.get(homeFile.getAbsolutePath(), "lib", VERSION_DIR_NAME + version).toFile();
            if (!libDir.exists()) {
                unZipToDir(verFile, libDir);
            }
            // 测试发现：在极少的情况下，可能出现解压后的文件有丢失，导致启动失败，手动解压 或 删除解压后的目录以触发重新解压 可解决问题。
            int fileCountExpected = getZipEntrySize(verFile);
            if (failedDocsCount > 0) { //PDMP-5252
                fileCountExpected = fileCountExpected - failedDocsCount;
            }

            int fileCountReal = sumFileCount(libDir) - 1;
            if (fileCountReal < fileCountExpected) {
                String msg = "The extracted version file may be incomplete and an attempt is being made to re-unzip it...";
                System.out.println(msg);
                System.err.println(msg);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                // 实际解压出的文件数少于期望的解压文件数量，则重新尝试一次
                unZipToDir(verFile, libDir);
                fileCountReal = sumFileCount(libDir) - 1;
                if (fileCountReal < fileCountExpected) {
                    msg = "The re-extracted version file may still be incomplete, it is recommended to check the package format of the version file or the JDK's decompression API to troubleshoot related issues.";
                    System.out.println(msg);
                    System.err.println(msg);
                }
            }
        }
    }

    static File getLibDir() {
        File versionFile = null;
        String version = "";
        File[] listFiles = new File(homeFile, "lib").listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (!file.isDirectory()) continue;

                String fileName = file.getName();
                if (!fileName.startsWith(VERSION_DIR_NAME)) continue;

                String ver = fileName.substring(VERSION_DIR_NAME.length());
                if (isLaterVersion(ver, version)) {
                    version = ver;
                    versionFile = file;
                }
            }
        }

        if (versionFile == null) {
            System.err.println("qingzhou library not found");
            System.exit(1);
        }
        return versionFile;
    }

    private static ClassLoader buildAdminClassLoader(File libDir) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(new File(new File(libDir, "command"), "qingzhou-command.jar").toURI().toURL());
        return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    private static void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = newFile(unZipDir, entry);
                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        boolean mkdirs = targetFile.mkdirs();
                        if (!mkdirs) {
                            throw new IOException("Failed to create directory " + targetFile);
                        }
                    }
                } else {
                    File parentFile = targetFile.getParentFile();
                    if (!parentFile.exists()) {
                        boolean mkdirs = parentFile.mkdirs();
                        if (!mkdirs) {
                            throw new IOException("Failed to create directory " + targetFile.getParentFile());
                        }
                    }
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        copyLargeStream(zip.getInputStream(entry), out);
                    } catch (Throwable ex) {
                        if (parentFile.getName().equals("docs")) { // PDMP-5252 某些环境下，docs文档的中文名称会导致启动解压失败
                            failedDocsCount++; // 忽略掉这个手册文件，别影响启动流程
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private static int getZipEntrySize(File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            return zip.size();
        }
    }

    private static int sumFileCount(File file) {
        int fileCount = 0;
        if (file.isFile()) {
            return 1;
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    fileCount += sumFileCount(f);
                }
            }
        }
        return fileCount + 1;
    }

    private static File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
            throw new IOException("Entry is outside of target dir:" + entry.getName()); // 防止压缩包解压目录穿越风险
        }
        return destFile;
    }

    private static void copyLargeStream(InputStream input, OutputStream output) throws IOException {

        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    private static boolean isLaterVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return false;

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
