package qingzhou;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Package {
    public static void main(String[] args) throws Exception {
        File versionDir = new File(args[0]);
        boolean devMode = Boolean.parseBoolean(System.getProperty("devMode"));

        System.out.println("版本目录：" + versionDir);

        addBuildTime(versionDir);
        generateLibZip(versionDir, devMode);

        System.out.println("打包完毕！");
    }

    private static void generateLibZip(File versionDir, boolean devMode) throws Exception {
        zipFiles(Objects.requireNonNull(versionDir.listFiles()),
                new File(versionDir.getParent(),
                        versionDir.getName()
                                + (devMode ? ("-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date())) : "")
                                + ".zip"));
        boolean delete = delete(versionDir);
        if (!delete) {
            throw new IllegalStateException(versionDir.toString());
        }
    }

    private static void addBuildTime(File libDir) throws IOException {
        String buildTime = "Build_Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        File releaseNotesFile = new File(libDir, "version-notes.md");
        List<String> fileContent = fileToString(releaseNotesFile);
        fileContent.add("");
        fileContent.add(buildTime);
        writeFile(releaseNotesFile, fileContent);
    }

    private static List<String> fileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            List<String> content = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                content.add(line);
            }
            return content;
        }
    }

    private static void writeFile(File file, List<String> fileLines) throws IOException {
        mkdirs(file.getParentFile());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
            StringBuilder context = new StringBuilder();
            fileLines.forEach(s -> context.append(s).append(System.lineSeparator()));
            bw.write(context.toString(), 0, context.length());
            bw.flush();
            bw.close();
        }
    }

    private static void mkdirs(File directory) throws IOException {
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

    private static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        return file.delete();
    }

    // 指定的多个文件压入到一个文件里
    private static void zipFiles(File[] srcFiles, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            for (File srcFile : srcFiles) {
                zipFile(srcFile, zos, srcFile.getName());
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

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }
}
