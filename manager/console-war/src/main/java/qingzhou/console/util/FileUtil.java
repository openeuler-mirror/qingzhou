package qingzhou.console.util;

import qingzhou.engine.util.Utils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        Utils.mkdirs(directory);
    }

    public static List<String> fileToLines(File file) throws IOException {
        List<String> lineList = new ArrayList<>();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineList.add(line);
            }
        }
        return lineList;
    }

    public static int fileTotalLines(File file) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
            reader.skip(Long.MAX_VALUE);
            return reader.getLineNumber() + 1;
        }
    }

    public static void writeFile(File file, String context) throws IOException {
        mkdirs(file.getParentFile());
        try (FileOutputStream fos = new FileOutputStream(file); BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            bw.write(context, 0, context.length());
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
        Utils.forceDelete(file);
    }

    // 复制文件或文件夹
    public static void copyFileOrDirectory(File from, File to) throws IOException {
        Utils.copyDirectory(from, to);
    }

    // 复制 文件夹

    public static File newFile(File first, String... more) {
        return Utils.newFile(first, more);
    }

    public static File newFile(String first, String... more) {
        return Utils.newFile(first, more);
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
