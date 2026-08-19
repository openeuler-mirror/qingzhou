package com.tongtech.zookeeper.starter.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类，仅保留 zookeeper 应用实际使用的方法。
 */
public class FileUtil {

    private FileUtil() {
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
        }
        for (String s : more) {
            if (s.contains("..")) {
                throw new IllegalArgumentException(s);
            }
        }
        return Paths.get(first, more).normalize().toFile();
    }

    public static List<String> readLines(File file) throws IOException {
        List<String> lineList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineList.add(line);
            }
        }
        return lineList;
    }
}
