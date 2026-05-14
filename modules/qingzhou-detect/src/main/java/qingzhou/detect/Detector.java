package qingzhou.detect;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class Detector {
    public static Set<String> detect(String[] scanPaths, String[] matchFiles) {
        Set<String> result = new LinkedHashSet<>();
        for (String path : scanPaths) {
            if (path == null || path.isEmpty()) continue;
            scan(new File(path), matchFiles, result);
        }
        return result;
    }

    private static void scan(File dir, String[] matches, Set<String> result) {
        if (dir == null || !dir.isDirectory() || !dir.canRead() || result.size() >= 10) return;
        if (match(dir, matches)) {
            result.add(dir.getAbsolutePath());
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) scan(f, matches, result);
        }
    }

    private static boolean match(File dir, String[] matches) {
        for (String m : matches) if (new File(dir, m).exists()) return true;
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Nginx: " + detect(
                new String[]{
                        System.getenv("NGINX_HOME"),
                        "/home/wangpl", "/usr/sbin",
                        "C:\\", "D:\\", "E:\\"
                },
                new String[]{"sbin/nginx", "sbin/nginx.exe"}
        ));
    }
}