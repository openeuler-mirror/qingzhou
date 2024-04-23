package qingzhou.engine.util;

import javassist.ClassPool;
import javassist.CtClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtil {

    public static String read(File file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return read(inputStream);
        }
    }

    public static String read(InputStream inputStream) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            fileContent.append(line);
        }

        return fileContent.toString();
    }

    public static Collection<String> detectAnnotatedClass(File[] libs, Class<?> annotationClass, String scopePrefix) throws Exception {
        Collection<String> targetClasses = new HashSet<>();
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendPathList(Arrays.stream(libs).map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        getScopeClasses(libs, scopePrefix).forEach(s -> {
            try {
                CtClass ctClass = classPool.get(s);
                if (ctClass.getAnnotation(annotationClass) != null) {
                    targetClasses.add(s);
                }
            } catch (Exception ignored) {
            }
        });
        return targetClasses;
    }

    private static Collection<String> getScopeClasses(File[] libs, String scopePrefix) throws IOException {
        Collection<String> scopeClasses = new HashSet<>();
        // 找出类名范围
        for (File file : libs) {
            if (!file.getName().endsWith(".jar")) continue;
            try (ZipFile jar = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    String entryName = zipEntry.getName();
                    int i = entryName.indexOf(".class");
                    if (i < 1) continue;
                    String className = entryName.substring(0, i).replace("/", ".");
                    if (scopePrefix != null && !className.startsWith(scopePrefix)) continue;

                    scopeClasses.add(className);
                }
            }
        }
        return scopeClasses;
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
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

    public static boolean notSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        //FilenameUtils.isSystemWindows()
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

    private FileUtil() {
    }

    public static LinkedHashMap<String, String> streamToProperties(InputStream inputStream) throws Exception {
        LinkedHashMap<String, String> data = new LinkedHashMap<>(); // 保持顺序
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int i = line.indexOf("=");
            if (i != -1) {
                String key = line.substring(0, i);
                String val = line.substring(i + 1);
                data.put(key, val);
            } else {
                data.put(line, "");
            }
        }
        return data;
    }
}