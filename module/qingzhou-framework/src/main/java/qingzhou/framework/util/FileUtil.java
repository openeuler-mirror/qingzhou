package qingzhou.framework.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    public static String getFileMD5(File file) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] fileDigest = getFileDigest(file, messageDigest);
        return HexUtil.bytesToHex(fileDigest);
    }

    public static byte[] getFileDigest(File file, MessageDigest messageDigest) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            FileChannel ch = in.getChannel();
            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            messageDigest.update(byteBuffer);
            return messageDigest.digest();
        }
    }

    public static boolean delete(File f) {
        if (f == null)
            return false;
        if (f.delete()) {
            return true;
        } else {
            System.err.println("Failed to delete: " + f);
            return false;
        }
    }

    public static void createNewFile(File newFile) {
        if (newFile == null)
            return;
        try {
            if (!newFile.createNewFile()) {
                System.err.println("Failed to createNewFile: " + newFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
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

    public static int fileTotalLines(File file) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
            reader.skip(Long.MAX_VALUE);
            return reader.getLineNumber() + 1;
        }
    }

    public static void writeFile(File file, String context) throws IOException {
        mkdirs(file.getParentFile());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
            bw.write(context, 0, context.length());
            bw.flush();
            bw.close();
        }
    }

    public static String fileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return ObjectUtil.inputStreamToString(fis, null);
        }
    }

    public static Properties fileToProperties(File file) throws Exception {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return ObjectUtil.streamToProperties(in);
        }
    }

    public static void forceDeleteQuietly(File file) {
        try {
            forceDelete(file);
        } catch (IOException ignored) {
        }
    }

    // 删除 文件夹
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!isSymlink(directory)) {
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
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
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
                StreamUtil.copyStream(in, zos);
            }
        }
    }

    // NOTO: 保持一致：VersionUtil
    public static void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = newFile(unZipDir, entry);
                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        boolean mkdirs = targetFile.mkdirs();
                        if (!mkdirs) {
                            new IllegalStateException("Failed to mkdirs: " + targetFile.getPath()).printStackTrace();
                        }
                    }
                } else {
                    if (!targetFile.getParentFile().exists()) {
                        boolean mkdirs = targetFile.getParentFile().mkdirs();
                        if (!mkdirs) {
                            new IllegalStateException("Failed to mkdirs: " + targetFile.getParentFile().getPath()).printStackTrace();
                        }
                    }
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        StreamUtil.copyStream(zip.getInputStream(entry), out);
                    }
                }
            }
        }
    }

    // NOTO: 保持一致：VersionUtil
    private static File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
            throw new IOException("Entry is outside of target dir:" + entry.getName());
        }
        return destFile;
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
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
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

    // 复制文件或文件夹
    public static void copyFileOrDirectory(File from, File to) throws IOException {
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
            try {
                // Files.copy 使用注意 当第二个参数to.toPath() 对应的文件正在读或写会抛出FileSystemException 另一个程序正在使用此文件，进程无法访问。
                Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (e instanceof FileSystemException) {
                    if (e.getMessage().contains("正在使用")) {
                        try (FileOutputStream fos = new FileOutputStream(to)) {
                            try (InputStream read = new BufferedInputStream(Files.newInputStream(from.toPath()), 32768)) {
                                StreamUtil.copyStream(read, fos);
                            }
                        }
                    }
                }
            }
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
            if (exclusionList == null || !exclusionList.contains(file.getCanonicalPath())) {
                if (file.isDirectory()) {
                    doCopyDirectory(file, copiedFile, exclusionList);
                } else {
                    copyFileOrDirectory(file, copiedFile);
                }
            }
        }
    }

    public static File newFile(File first, String... more) {
        return newFile(first.getAbsolutePath(), more);
    }

    public static File newFile(String first, String... more) {
        if (first.contains("..")) {
            throw new IllegalArgumentException(first);
        }

        if (more == null || more.length == 0 || more[0] == null) {
            return Paths.get(first).normalize().toFile();
        } else {
            for (String s : more) {
                if (s.contains("..")) {
                    throw new IllegalArgumentException(s);
                }
            }
            return Paths.get(first, more).normalize().toFile();
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

    /**
     * 获取文件大小
     *
     * @param file
     * @return xx B、xx K、xx M、xx G、xx T
     */
    public static String getFileSize(File file) {
        long fileLength = file.exists() ? getFileLength(file) : 0L;
        String fileSize = fileLength + " B";
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        long tb = gb * 1024;
        long eb = tb * 1024;
        if (fileLength >= kb && fileLength < mb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(kb)).setScale(1, BigDecimal.ROUND_UP) + "K";
        } else if (fileLength >= mb && fileLength < gb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(mb)).setScale(1, BigDecimal.ROUND_DOWN) + "M";
        } else if (fileLength >= gb && fileLength < tb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(gb)).setScale(2, BigDecimal.ROUND_DOWN) + "G";
        } else if (fileLength >= tb && fileLength < eb) {
            fileSize = new BigDecimal(fileLength).divide(new BigDecimal(tb)).setScale(2, BigDecimal.ROUND_DOWN) + "T";
        }
        return fileSize;
    }

    public static List<String> readLines(final File file, final Charset charset) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return readLines(in, charset);
        }
    }

    public static List<String> readLines(final InputStream input, final Charset cs) throws IOException {
        try (final InputStreamReader reader = new InputStreamReader(input, cs != null ? cs : StandardCharsets.UTF_8)) {
            return readLines(reader);
        }
    }

    public static List<String> readLines(final Reader input) throws IOException {
        try (final BufferedReader reader = (input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input))) {
            final List<String> list = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            return list;
        }
    }

    public static Properties getProperties(File file) throws Exception {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return ObjectUtil.streamToProperties(in);
        }
    }

    private FileUtil() {
    }
}
