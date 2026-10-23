package qingzhou.common;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    // 确保创建一个文件夹
    public static void mkdirs(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "File " + directory + " exists and is " + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    String message = "Unable to create directory " + directory;
                    throw new IOException(message);
                }
            }
        }
    }

    // 剪切文件或文件夹
    public static void moveFileOrDirectory(File srcFile, File desFile) throws IOException {
        if (srcFile == null || desFile == null || !srcFile.exists() || srcFile.getCanonicalFile().equals(desFile.getCanonicalFile()))
            return;

        File desParent = desFile.getParentFile();
        mkdirs(desParent);

        if (srcFile.isFile()) {
            Files.move(srcFile.toPath(), desFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            if (!srcFile.renameTo(desFile)) {
                // failed to rename the file, try copying the stream instead.
                copyFileOrDirectory(srcFile, desFile);
                forceDelete(srcFile);
            }
        }
    }


    // 复制文件或文件夹
    public static void copyFileOrDirectory(File from, File to) throws IOException {
        if (from.getCanonicalFile().equals(to.getCanonicalFile())) return;

        if (from.isDirectory()) {
            copyDirectory(from, to);
        } else {
            if (!to.getParentFile().exists()) {
                mkdirs(to.getParentFile());
            }
            Path source = from.toPath();
            Path target = to.toPath();
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES); // 应用增量更新：COPY_ATTRIBUTES 保留时间戳
            } catch (FileSystemException e) {
                // 文件被占用（Windows 常见）时退回流复制；复制再失败必须上抛：
                // 原实现以本地化错误文案判断是否重试，其它失败一律静默返回，调用方会误判复制成功
                try (FileOutputStream fos = new FileOutputStream(to);
                     InputStream read = new BufferedInputStream(Files.newInputStream(source), 32768)) {
                    copyStream(read, fos);
                }
            }
            copyFileTimes(source, target);
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

    // COPY_ATTRIBUTES 可能不生效，显式再设一次时间戳兜底：
    public static void copyFileTimes(Path source, Path target) {
        if (source == null || !source.toFile().exists() ||
                target == null || !target.toFile().exists()) return;
        try {
            BasicFileAttributes sourceAttrs = Files.readAttributes(source, BasicFileAttributes.class);
            Files.getFileAttributeView(target, BasicFileAttributeView.class)
                    .setTimes(
                            sourceAttrs.lastModifiedTime(),
                            sourceAttrs.lastAccessTime(),
                            sourceAttrs.creationTime()
                    );
        } catch (IOException e) {
            // 时间戳复制失败不致命，忽略
        }
    }

    // 复制 文件夹
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
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }

        // 目标目录位于源目录内部时需排除自身，避免递归复制自身
        List<String> exclusionList = getExclusionList(srcDir, destDir);
        doCopyDirectory(srcDir, destDir, exclusionList);
    }

    private static List<String> getExclusionList(File srcDir, File destDir) throws IOException {
        // 仅当 destDir 位于 srcDir 内部时排除目标目录，避免复制自身导致死循环；
        // 按规范路径 + 目录分隔符比较：直接前缀比较会把 /a/app2 误判为 /a/app 的子目录
        if (!destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath() + File.separator)) {
            return null;
        }
        List<String> exclusionList = new ArrayList<>(1);
        exclusionList.add(destDir.getCanonicalPath());
        return exclusionList;
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
            mkdirs(destDir);
        }
        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        for (File file : files) {
            File copiedFile = new File(destDir, file.getName());
            if (exclusionList == null || !exclusionList.contains(file.getCanonicalPath())) {
                if (file.isDirectory()) {
                    doCopyDirectory(file, copiedFile, exclusionList);
                } else {
                    copyFileOrDirectory(file, copiedFile);
                }
            }
        }
    }

    /**
     * 敏感文件（密钥等）权限收紧为仅属主可读写：默认 umask 下同机其它用户可读，暴露即等同泄露。
     * 非 POSIX 平台无权限模型、或无权限修改时保持原状，由调用方决定是否仅告警。
     */
    public static void restrictToOwnerOnly(File file, Consumer<String> warning) {
        if (file == null || !file.isFile()) return;
        try {
            Set<PosixFilePermission> current = Files.getPosixFilePermissions(file.toPath());
            Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
            if (current.equals(ownerOnly)) return;

            warning.accept("file permission is " + PosixFilePermissions.toString(current)
                    + ", tightened to rw-------: " + file);
            Files.setPosixFilePermissions(file.toPath(), ownerOnly);
        } catch (UnsupportedOperationException e) {
            // Windows 等无 POSIX 权限模型的平台：交由文件系统 ACL 控制
        } catch (IOException e) {
            warning.accept("failed to tighten file permission to rw-------: " + file + ", " + e.getMessage());
        }
    }

    // 删除 文件 或 文件夹，若失败也不抛异常
    public static void forceDeleteQuietly(File file) {
        try {
            forceDelete(file);
        } catch (IOException ignored) {
        }
    }

    // 删除 文件 或 文件夹
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (file.exists() && !file.delete()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                if (!file.delete()) {
                    throw new IOException("Unable to delete file: " + file);
                }
            }
        }
    }

    // 删除 文件夹
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!isSymlink(directory)) {
            cleanDirectory(directory, false);
        }

        if (!directory.delete()) {
            String message =
                    "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    // 将 文件夹 清空
    public static void cleanDirectory(File directory, boolean ignoredException) throws IOException {
        try {
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

            for (File file : files) {
                if (ignoredException) {
                    forceDeleteQuietly(file);
                } else {
                    // 直接抛出异常，不继续清理
                    forceDelete(file);
                }
            }
        } catch (Exception e) {
            if (!ignoredException) {
                throw e;
            }
        }
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     * <p>
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     * <p>
     * <b>Note:</b> the current implementation always returns {@code false} if
     * the system is detected as Windows using
     * {@link File#separatorChar} == '\\'
     *
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     * @since 2.0
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        //FilenameUtils.isSystemWindows()
        if (File.separatorChar == '\\') {
            return false;
        }
        File testDir;
        if (file.getParent() == null) {
            testDir = file;
        } else {
            File parent = file.getParentFile().getCanonicalFile();
            testDir = new File(parent, file.getName());
        }

        return !testDir.getCanonicalFile().equals(testDir.getAbsoluteFile());
    }
}
