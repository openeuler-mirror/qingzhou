package qingzhou.framework.util;

import qingzhou.bootstrap.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {
    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    public static void createNewFile(File newFile) {
        if (newFile == null) return;
        if (newFile.exists()) return;
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

    public static void writeFile(File file, String context) throws IOException {
        mkdirs(file.getParentFile());
        try (FileOutputStream fos = new FileOutputStream(file); BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            bw.write(context, 0, context.length());
        }
    }

    public static Properties fileToProperties(File file) throws Exception {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return streamToProperties(in);
        }
    }

    public static void forceDeleteQuietly(File file) {
        try {
            forceDelete(file);
        } catch (IOException ignored) {
        }
    }

    public static void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ, StandardCharsets.UTF_8)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = newFile(unZipDir, entry);
                if (entry.isDirectory()) {
                    if (!targetFile.exists()) {
                        boolean mkdirs = targetFile.mkdirs();
                        if (!mkdirs) {
                            throw new IllegalStateException("Failed to mkdirs: " + targetFile.getPath());
                        }
                    }
                } else {
                    if (!targetFile.getParentFile().exists()) {
                        boolean mkdirs = targetFile.getParentFile().mkdirs();
                        if (!mkdirs) {
                            throw new IllegalStateException("Failed to mkdirs: " + targetFile.getParentFile().getPath());
                        }
                    }
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        copyStream(zip.getInputStream(entry), out);
                    }
                }
            }
        }
    }

    private static File newFile(File destDir, ZipEntry entry) throws IOException {
        File destFile = new File(destDir, entry.getName());
        if (!destFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
            throw new IOException("Entry is outside of target dir:" + entry.getName());
        }
        return destFile;
    }

    // 删除 文件 或 文件夹
    public static void forceDelete(File file) throws IOException {
        Utils.forceDelete(file);
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
                                copyStream(read, fos);
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
        return Utils.newFile(first, more);
    }

    private FileUtil() {
    }

    public static Properties streamToProperties(InputStream inputStream) throws Exception {
        return Utils.streamToProperties(inputStream);
    }
}
