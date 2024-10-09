package qingzhou;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Package {
    public static void main(String[] args) throws Exception {
        System.out.println("设置构建时间...");
        addBuildTime(new File(args[0]), args[1]);
        System.out.println("设置构建时间完毕！");
    }

    private static void addBuildTime(File libDir, String zipPath) throws IOException {
        String buildTime = "Build_Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        File releaseNotesFile = new File(libDir, "version-notes.md");
        String fileContent = fileToString(releaseNotesFile);
        fileContent += (System.lineSeparator() + buildTime);
        writeFile(releaseNotesFile, fileContent);

        //处理zip包
        processZip(zipPath, fileContent);
    }

    private static void processZip(String zipFilePath, String newContent) {

        try {
            // 创建输入流和输出流
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath + ".tmp")))) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (entryName.endsWith("version-notes.md")) {
                        // 修改文件内容
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.write(newContent.getBytes());
                        zos.closeEntry();
                    } else {
                        // 复制原始文件内容到 ByteArrayOutputStream
                        baos.reset();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }

                        // 将原始文件写入新的 ZIP
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.write(baos.toByteArray());
                        zos.closeEntry();
                    }
                    zis.closeEntry();
                }
            }

            // 替换原始 ZIP 文件
            new File(zipFilePath).delete();
            new File(zipFilePath + ".tmp").renameTo(new File(zipFilePath));

        } catch (IOException e) {
            //
        }
    }

    private static String fileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return inputStreamToString(fis, null);
        }
    }

    private static String inputStreamToString(InputStream is, Charset cs) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, cs != null ? cs : StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            content.append(line).append(System.lineSeparator());
        }
        return content.toString();
    }

    private static void writeFile(File file, String context) throws IOException {
        mkdirs(file.getParentFile());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
            bw.write(context, 0, context.length());
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
}
