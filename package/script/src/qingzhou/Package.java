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

/**
 * 主类，用于打包指定目录并生成zip文件。
 */
public class Package {

    /**
     * 主方法，程序的入口点。
     *
     * @param args 命令行参数，第一个参数是版本目录的路径，第二个参数是开发模式的标志（true/false）。
     * @throws Exception 抛出可能发生的异常。
     */
    public static void main(String[] args) throws Exception {
        File versionDir = new File(args[0]);
        boolean devMode = Boolean.parseBoolean(args[1]);

        System.out.println("版本目录：" + versionDir);

        addBuildTime(versionDir);// 添加构建时间到版本说明文件
        generateLibZip(versionDir, devMode); //生成lib的zip文件

        System.out.println("打包完毕！");
    }

    /**
     * 生成lib的zip文件，并在生成后删除原始目录。
     *
     * @param versionDir 版本目录。
     * @param devMode    开发模式标志。
     * @throws Exception 抛出可能发生的异常。
     */
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

    /**
     * 在版本说明文件中添加构建时间。
     *
     * @param libDir 版本目录。
     * @throws IOException 抛出可能发生的IO异常。
     */
    private static void addBuildTime(File libDir) throws IOException {
        String buildTime = "Build_Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        File releaseNotesFile = new File(libDir, "version-notes.md");
        List<String> fileContent = fileToString(releaseNotesFile);
        fileContent.add("");
        fileContent.add(buildTime);
        writeFile(releaseNotesFile, fileContent);
    }

    /**
     * 将文件内容读取为字符串列表。
     *
     * @param file 要读取的文件。
     * @return 文件内容的字符串列表。
     * @throws IOException 抛出可能发生的IO异常。
     */
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

    /**
     * 将字符串列表写入文件。
     *
     * @param file    要写入的文件。
     * @param fileLines 要写入的字符串列表。
     * @throws IOException 抛出可能发生的IO异常。
     */
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

    /**
     * 确保目录存在，如果不存在则创建。
     *
     * @param directory 要确保存在的目录。
     * @throws IOException 抛出可能发生的IO异常。
     */
    private static void mkdirs(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "File " + directory + " exists and is " + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                 // 检查是否由其他线程或进程创建了目录
                if (!directory.isDirectory()) {
                    String message = "Unable to create directory " + directory;
                    throw new IOException(message);
                }
            }
        }
    }

    /**
     * 删除文件或目录（递归删除）。
     *
     * @param file 要删除的文件或目录。
     * @return 如果删除成功返回true，否则返回false。
     */
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

    /**
     * 将指定的多个文件压缩到一个zip文件中。
     *
     * @param srcFiles 要压缩的文件数组。
     * @param zipFile  目标zip文件。
     * @throws IOException 抛出可能发生的IO异常。
     */
    private static void zipFiles(File[] srcFiles, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            for (File srcFile : srcFiles) {
                zipFile(srcFile, zos, srcFile.getName());
            }
        }
    }

    /**
     * 将文件或目录添加到zip输出流中。
     *
     * @param srcFile   要添加的文件或目录。
     * @param zos       zip输出流。
     * @param toZipName 在zip文件中的名称。
     * @throws IOException 抛出可能发生的IO异常。
     */
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

    /**
     * 从输入流复制数据到输出流。
     *
     * @param input  输入流。
     * @param output 输出流。
     * @throws IOException 抛出可能发生的IO异常。
     */
    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }
}
