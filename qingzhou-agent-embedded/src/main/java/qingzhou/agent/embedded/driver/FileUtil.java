package qingzhou.agent.embedded.driver;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    public static void zipFiles(File srcFile, File zipFile, boolean containsBaseDir) throws IOException {
        zipFiles(new File[]{srcFile}, zipFile, containsBaseDir);
    }

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

    public static String getFileSize(File file) {
        long fileLength = file.exists() ? getFileLength(file) : 0L;
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        long tb = gb * 1024;
        String fileSize = fileLength + " B";
        if (fileLength >= kb && fileLength < mb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(kb), 1, RoundingMode.UP) + "K";
        } else if (fileLength >= mb && fileLength < gb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(mb), 1, RoundingMode.DOWN) + "M";
        } else if (fileLength >= gb && fileLength < tb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(gb), 2, RoundingMode.DOWN) + "G";
        } else if (fileLength >= tb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(tb), 2, RoundingMode.DOWN) + "T";
        }
        return fileSize;
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

    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) return;
        if (File.separatorChar != '\\') {
            if (notSymlink(directory)) {
                cleanDirectory(directory);
            }
        } else {
            cleanDirectory(directory);
        }
        if (!directory.delete()) {
            throw new IOException("Unable to delete directory " + directory);
        }
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists())
            throw new IllegalArgumentException(directory + " does not exist");
        if (!directory.isDirectory())
            throw new IllegalArgumentException(directory + " is not a directory");
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Failed to list contents of " + directory);
        for (File file : files) {
            forceDelete(file);
        }
    }

    public static void copyFileOrDirectory(File from, File to) throws IOException {
        if (from.isDirectory()) {
            copyDirectory(from, to);
        } else {
            if (!to.getParentFile().exists()) {
                to.getParentFile().mkdirs();
            }
            try {
                Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (e instanceof FileSystemException && e.getMessage() != null
                        && e.getMessage().contains("正在使用")) {
                    try (FileOutputStream fos = new FileOutputStream(to);
                         InputStream read = new BufferedInputStream(Files.newInputStream(from.toPath()), 32768)) {
                        copyStream(read, fos);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private static void copyDirectory(File srcDir, File destDir) throws IOException {
        if (!srcDir.exists()) throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        if (!srcDir.isDirectory()) throw new IOException("Source '" + srcDir + "' is not a directory");
        if (!destDir.exists() && !destDir.mkdirs())
            throw new IOException("Cannot create destination directory");
        File[] files = srcDir.listFiles();
        if (files == null) throw new IOException("Failed to list contents of " + srcDir);
        for (File file : files) {
            File copiedFile = new File(destDir, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, copiedFile);
            } else {
                copyFileOrDirectory(file, copiedFile);
            }
        }
    }

    private static boolean notSymlink(File file) throws IOException {
        if (File.separatorChar == '\\') return true;
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