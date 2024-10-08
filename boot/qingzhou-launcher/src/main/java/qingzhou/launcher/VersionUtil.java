package qingzhou.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class VersionUtil {
    private final String qzVerName = "version";
    private File qingzhouHomeFile;
    private File versionFile = null;

    File getLibDir() {
        if (versionFile != null) {
            return versionFile;
        }

        String version = "";
        File libDir = new File(getHomeDir(), "lib");
        File[] listFiles = libDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (!file.isDirectory())
                    continue;

                String fileName = file.getName();
                if (!fileName.startsWith(qzVerName))
                    continue;

                String ver = fileName.substring(qzVerName.length());
                if (isLaterVersion(ver, version)) {
                    version = ver;
                    versionFile = file;
                }
            }
        }

        if (versionFile == null) {
            versionFile = new File(libDir, qzVerName);
            if (!versionFile.isDirectory()) {
                throw new IllegalStateException("Qingzhou " + qzVerName + " file not found !!!");
            }
        }

        return versionFile;
    }

    void prepare() throws IOException {
        if (versionFile != null) {
            return;
        }

        // version 脚本不显示补丁号问题
        File qzVerDir = new File(getHomeDir(), "lib");
        String version = "";
        String format = ".zip";
        File[] listFiles = qzVerDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                String fileName = file.getName();
                if (fileName.startsWith(qzVerName) && fileName.endsWith(format)) {
                    String ver = fileName.substring(qzVerName.length(), fileName.length() - format.length());
                    if (isLaterVersion(ver, version)) {
                        version = ver;
                    }
                }
            }
        }

        File verFile = new File(qzVerDir, qzVerName + version + format);
        if (verFile.exists()) {
            File libDir = new File(qzVerDir, qzVerName + version);
            if (!libDir.exists()) {
                unZipToDir(verFile, libDir);
            }
            // 测试发现：在极少的情况下，可能出现解压后的文件有丢失，导致启动失败，手动解压 或 删除解压后的目录以触发重新解压 可解决问题。
            int fileCountExpected = getZipEntrySize(verFile);
            int fileCountReal = sumFileCount(libDir) - 1;
            if (fileCountReal < fileCountExpected) {
                String msg = "The extracted version file may be incomplete and an attempt is being made to re-unzip it...";
                Launcher.log(msg);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 实际解压出的文件数少于期望的解压文件数量，则重新尝试一次
                unZipToDir(verFile, libDir);
                fileCountReal = sumFileCount(libDir) - 1;
                if (fileCountReal < fileCountExpected) {
                    msg = "The re-extracted version file may still be incomplete, "
                            + "it is recommended to check the package format of the version file "
                            + "or the JDK's decompression API to troubleshoot related issues.";
                    Launcher.log(msg);
                }
            }
        }
    }

    private File getHomeDir() {
        if (qingzhouHomeFile == null) {
            String jarPath = VersionUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            try { // 兼容中文路径
                jarPath = URLDecoder.decode(
                        jarPath,
                        Charset.defaultCharset().name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String flag = "/bin/qingzhou-launcher.jar";
            int i = jarPath.indexOf(flag);
            String pre = jarPath.substring(0, i);
            try {
                qingzhouHomeFile = new File(pre).getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return qingzhouHomeFile;
    }

    private void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = newFile(unZipDir, entry);
                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        Launcher.mkdirs(targetFile);
                    }
                } else {
                    if (!targetFile.getParentFile().exists()) {
                        Launcher.mkdirs(targetFile.getParentFile());
                    }
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        copyLargeStream(zip.getInputStream(entry), out);
                    }
                }
            }
        }
    }

    private int getZipEntrySize(File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            return zip.size();
        }
    }

    private File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
            throw new IllegalStateException("Entry is outside of target dir:" + entry.getName());
        }
        return destFile;
    }

    private void copyLargeStream(InputStream input, OutputStream output) throws IOException {

        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    private int sumFileCount(File file) {
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

    private boolean isLaterVersion(String v1, String v2) {
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
