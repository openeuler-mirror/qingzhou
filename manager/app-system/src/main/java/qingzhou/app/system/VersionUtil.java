package qingzhou.app.system;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VersionUtil {

    private static final String RELEASE_NOTES_FILE = "release-notes.txt";

    private static File versionFile;

    public final static String qzVerName = "version";

    public final static String format = ".zip";

    public static LinkedList<Map<String, String>> versionList() {
        LinkedList<Map<String, String>> result = new LinkedList<>();
        Set<String> versionList = new HashSet<>();
        Map<String, String> map = new HashMap<>();
        File libDir = new File(getHomeDir(), "lib");
        File[] listFiles = libDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                String fileName = file.getName();
                if (!fileName.startsWith(qzVerName)) {
                    continue;
                }
                String ver = getVer(fileName);
                versionList.add(ver);
                if (!map.containsKey(ver)) {
                    map.put(ver, getReleaseNotes(file));
                }
            }
        }
        List<String> versions = versionList.stream().sorted(VersionUtil::isLaterVersion).collect(Collectors.toList());
        for (String ver : versions) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("version", ver);
            hashMap.put("releaseNotes", map.get(ver));
            hashMap.put("running", String.valueOf(getLibDir().getName().equals(qzVerName + ver)));
            result.add(hashMap);
        }
        return result;
    }

    public static File getHomeDir() {
        String jarPath = VersionUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            jarPath = URLDecoder.decode( // 兼容中文路径
                    jarPath,
                    Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException ignored) {
        }
        final File file = new File(jarPath);
        return file.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
    }

    public static int isLaterVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        v1 = v1.trim();
        v2 = v2.trim();
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");

        int len1 = arr1.length;
        int len2 = arr2.length;
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            String c1 = arr1[k];
            String c2 = arr2[k];
            if (!c1.equals(c2)) {
                try {
                    return Integer.parseInt(c1) > Integer.parseInt(c2) ? 1 : -1;
                } catch (NumberFormatException e) {
                    return c1.compareTo(c2) > 0 ? 1 : -1;
                }
            }
            k++;
        }
        return len1 > len2 ? 1 : -1;
    }

    public static String getReleaseNotes(File versionDir) {
        String fileName = versionDir.getName();
        try {
            if (versionDir.isFile()) {
                String format = ".zip";
                boolean isZip = fileName.toLowerCase().endsWith(format);
                if (isZip) {
                    try (ZipFile zipFile = new ZipFile(versionDir)) {
                        ZipEntry entry = zipFile.getEntry(RELEASE_NOTES_FILE);
                        if (entry != null) {
                            return inputStreamToString(zipFile.getInputStream(entry), null);
                        }
                    }
                }
            } else if (versionDir.isDirectory()) {
                File notesFile = new File(versionDir, RELEASE_NOTES_FILE);
                if (notesFile.exists()) {
                    return fileToString(notesFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String inputStreamToString(InputStream is, Charset cs) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, cs != null ? cs : StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            content.append(line).append(System.lineSeparator());
        }
        return content.toString();
    }

    public static String fileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return inputStreamToString(fis, null);
        }
    }

    public static File getLibDir() {
        if (versionFile != null) {
            return versionFile;
        }
        String version = "";
        File libDir = new File(getHomeDir(), "lib");
        File[] listFiles = libDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (!file.isDirectory())
                    continue;

                String fileName = file.getName();
                if (!fileName.startsWith(qzVerName))
                    continue;

                String ver = fileName.substring(qzVerName.length());
                if (isLaterVersion(ver, version) == 1) {
                    version = ver;
                    versionFile = file;
                }
            }
        }

        if (versionFile == null) {
            versionFile = new File(libDir, qzVerName);
            if (!versionFile.isDirectory()) {
                throw new IllegalStateException("QingZhou " + qzVerName + " file not found !!!");
            }
        }

        return versionFile;
    }

    public static String getVer(String fileName) {
        String ver;
        String format = ".zip";
        if (fileName.endsWith(format)) {
            ver = fileName.substring(qzVerName.length(), fileName.length() - format.length());
        } else {
            ver = fileName.substring(qzVerName.length());
        }
        return ver;
    }
}
