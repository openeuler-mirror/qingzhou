package qingzhou.app.driver;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    // 解压上限：条目数与累计解压字节双限，防高压缩比包（zip bomb）写爆磁盘与 inode
    private static final int MAX_UNZIP_ENTRIES = 100_000;
    private static final long MAX_UNZIP_BYTES = 512L * 1024 * 1024;

    /**
     * 解压 zip 到目标目录（防 zip-slip：目录穿越条目直接拒绝）。
     *
     * @param overwrite true 覆盖已存在项（如驱动覆盖应用同名类）；false 跳过已存在项（如保留应用自带 MANIFEST）
     */
    public static void unZipToDir(File srcFile, File unZipDir, boolean overwrite) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ)) {
            String baseDir = unZipDir.getCanonicalPath() + File.separator;
            long totalBytes = 0;
            int entryCount = 0;
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                if (++entryCount > MAX_UNZIP_ENTRIES) {
                    throw new IOException("too many entries in: " + srcFile);
                }
                ZipEntry entry = e.nextElement();
                File targetFile = new File(unZipDir, entry.getName());
                if (!targetFile.getCanonicalPath().startsWith(baseDir)) {
                    throw new IOException("Entry is outside of target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!targetFile.mkdirs() && !targetFile.isDirectory()) {
                        throw new IOException("cannot create dir: " + targetFile);
                    }
                } else {
                    // 应用解压跳过已存在项：应用侧的 META-INF/MANIFEST.MF 不得被 driver 覆盖；
                    // driver 解压（overwrite=true）允许覆盖，保证驱动实现始终以框架自带版本为准
                    if (overwrite || !Files.exists(targetFile.toPath())) {
                        File parent = targetFile.getParentFile();
                        if (parent != null) parent.mkdirs();
                        try (InputStream in = zip.getInputStream(entry);
                             OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                            totalBytes += copyStreamWithLimit(in, out, MAX_UNZIP_BYTES - totalBytes);
                        }
                    }
                }
            }
        }
    }

    // 带累计上限的拷贝：实际写出量超限立即中止（entry.getSize() 可能为 -1，不能作为依据）
    private static long copyStreamWithLimit(InputStream input, OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        long written = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            written += n;
            if (written > maxBytes) throw new IOException("unzip exceeds max bytes: " + MAX_UNZIP_BYTES);
            output.write(buffer, 0, n);
        }
        output.flush();
        return written;
    }

    // 压缩一个文件或文件夹
    public static void zipFiles(File srcFile, File zipFile, boolean containsBaseDir) throws IOException {
        zipFiles(new File[]{srcFile}, zipFile, containsBaseDir);
    }

    // 指定的多个文件压入到一个文件里
    public static void zipFiles(File[] srcFiles, File zipFile, boolean containsBaseDir) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            for (File srcFile : srcFiles) {
                if (!srcFile.isDirectory() || containsBaseDir) {
                    zipFile(srcFile, zos, srcFile.getName());
                } else {
                    File[] listFiles = srcFile.listFiles();
                    if (listFiles != null) {
                        for (File sub : listFiles) {
                            zipFile(sub, zos, sub.getName());
                        }
                    }
                }
            }
        }
    }

    private static void zipFile(File srcFile, ZipOutputStream zos, String toZipName) throws IOException {
        if (srcFile.isDirectory()) {
            zos.putNextEntry(new ZipEntry(toZipName + "/"));
            File[] listFiles = srcFile.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    zipFile(file, zos, toZipName + "/" + file.getName());
                }
            }
        } else {
            zos.putNextEntry(new ZipEntry(toZipName));
            try (InputStream in = Files.newInputStream(srcFile.toPath())) {
                copyStream(in, zos);
            }
        }
    }

    public static long getFileLength(File file) {
        if (file.isDirectory()) {
            long length = 0;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    length += getFileLength(f);
                }
            }
            return length;
        } else {
            return file.length();
        }
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    public static void forceDeleteQuietly(File file) {
        try {
            forceDelete(file);
        } catch (IOException ignored) {
        }
    }

    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (file.exists() && !file.delete()) {
                // Windows 下文件句柄释放有延迟，短间隔重试；其它平台立即失败，不空耗
                if (IS_WINDOWS) {
                    for (int i = 0; i < 10; i++) {
                        sleep(200);
                        if (file.delete()) return;
                    }
                }
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (notSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    // 将 文件夹 清空
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                if (exception == null) exception = ioe; // 保留首个失败：继续清理其余文件，不丢失最初的失败原因
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    public static void mkdirs(File directory) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "File " + directory + " exists and is not a directory. Unable to create directory.";
                throw new IllegalStateException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    String message = "Unable to create directory " + directory;
                    throw new IllegalStateException(message);
                }
            }
        }
    }

    public static void copyFileOrDirectory(File from, File to) throws IOException {
        if (from.isDirectory()) {
            copyDirectory(from, to);
        } else {
            if (!to.getParentFile().exists()) {
                mkdirs(to.getParentFile());
            }

            try {
                // Files.copy 使用注意 当第二个参数to.toPath() 对应的文件正在读或写会抛出FileSystemException 另一个程序正在使用此文件，进程无法访问。
                Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (FileSystemException e) { // 文件被占用时，等待句柄释放后改用流拷贝
                copyByStream(from, to);
            }
        }
    }

    // Windows 下文件句柄释放有延迟，等待后改用流拷贝重试；失败必须抛出，不能静默跳过导致文件缺失
    private static void copyByStream(File from, File to) throws IOException {
        if (IS_WINDOWS) sleep(2000);
        try (InputStream read = new BufferedInputStream(Files.newInputStream(from.toPath()), 32768);
             OutputStream out = Files.newOutputStream(to.toPath())) {
            copyStream(read, out);
        }
    }

    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcDir.exists()) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getAbsolutePath().equals(destDir.getAbsolutePath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }

        List<String> exclusionList = getExclusionList(srcDir, destDir);
        doCopyDirectory(srcDir, destDir, exclusionList);
    }

    private static void doCopyDirectory(File srcDir, File destDir, List<String> exclusionList) throws IOException {
        File[] files = srcDir.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        for (File file : files) {
            File copiedFile = new File(destDir, file.getName());
            if (exclusionList == null || !exclusionList.contains(file.getAbsolutePath())) {
                if (file.isDirectory()) {
                    doCopyDirectory(file, copiedFile, exclusionList);
                } else {
                    copyFileOrDirectory(file, copiedFile);
                }
            }
        }
    }

    private static List<String> getExclusionList(File srcDir, File destDir) throws IOException {
        List<String> exclusionList = null;
        // 按规范路径 + 目录分隔符比较：直接前缀比较会把 /a/app2 误判为 /a/app 的子目录
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath() + File.separator)) {
            File[] srcFiles = srcDir.listFiles();
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList<>(srcFiles.length);
                for (File srcFile : srcFiles) {
                    File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getAbsolutePath());
                }
            }
        }
        return exclusionList;
    }

    private static boolean notSymlink(File file) throws IOException {
        if (File.separatorChar == '\\') {
            return true;
        }
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }
}
