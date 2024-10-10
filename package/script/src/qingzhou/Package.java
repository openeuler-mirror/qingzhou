package qingzhou;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Package {
    public static void main(String[] args) throws Exception {
        System.out.println("设置构建时间...");
        addBuildTime(new File(args[0]));

        System.out.println("打包完毕！");
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
}
