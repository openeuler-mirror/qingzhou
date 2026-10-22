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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
            //            try (FileOutputStream fos = new FileOutputStream(to)) {
            //                try (InputStream read = new BufferedInputStream(new FileInputStream(from), 32768)) {
            //                    copyStream(read, fos);
            //                }
            //            }

            // 注释了上面的旧方式，使用新方式：
            /*
               Files.copy 使用注意 当第二个参数to.toPath() 对应的文件正在读或写会抛出FileSystemException
               另一个程序正在使用此文件，进程无法访问。
             */
            Path source = from.toPath();
            Path target = to.toPath();
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES); // 应用增量更新：COPY_ATTRIBUTES 保留时间戳
            } catch (IOException e) {
                if (e instanceof FileSystemException) {
                    if (e.getMessage() != null && e.getMessage().contains("正在使用")) {
                        try (FileOutputStream fos = new FileOutputStream(to)) {
                            try (InputStream read = new BufferedInputStream(Files.newInputStream(source), 32768)) {
                                copyStream(read, fos);
                            }
                        }
                    }
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

    // for #PDMP-5917 手动复制时间戳（确保兼容性），如果 COPY_ATTRIBUTES 不生效，可以显式读取并设置时间戳：
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
            // 有时候会出错，没必要打印日志
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

        // Cater for destination being directory within the source directory (see IO-141)
        List<String> exclusionList = getExclusionList(srcDir, destDir);
        doCopyDirectory(srcDir, destDir, exclusionList);
    }

    private static List<String> getExclusionList(File srcDir, File destDir) throws IOException {
        List<String> exclusionList = null;
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
            File[] srcFiles = srcDir.listFiles();
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList<>(srcFiles.length);
                for (File srcFile : srcFiles) {
                    File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
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
            mkdirs(destDir);// 不用判断返回值，有时候返回false,实际是成功的
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
                    Thread.sleep(2000);// for #ITAIT-4164
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
                    //直接抛出异常，不继续循环了ITAIT-6618
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
