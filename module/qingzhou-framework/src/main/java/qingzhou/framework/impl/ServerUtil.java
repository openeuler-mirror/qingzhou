package qingzhou.framework.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServerUtil {// todo：将无状态的工具方法，拆分到对应的 util/*Utils类里面
    private static Boolean isWindows;
    private static File domain;
    private static File dataXmlFile;
    private static File libDir;
    private static File home;

    /**
     * 字符串是否包含中文
     */
    public static boolean containsZHChar(String str) {
        if (str == null) return false;
        str = str.trim();
        if (str.isEmpty()) return false;

        Pattern p = Pattern.compile("[\u4E00-\u9FA5\\！\\，\\。\\（\\）\\《\\》\\“\\”\\？\\：\\；\\【\\】]");
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static Properties streamToProperties(InputStream inputStream) throws Exception {
        Properties properties = new Properties();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int i = line.indexOf("=");
            if (i != -1) {
                String key = line.substring(0, i);
                String val = line.substring(i + 1);
                properties.setProperty(key, val);
            } else {
                properties.setProperty(line, "");
            }
        }
        return properties;
    }

    public static <S> List<S> loadServices(String serviceType, ClassLoader classLoader) {
        List<S> services = new ArrayList<>();

        try {
            ClassLoader loader = null;
            if (classLoader != null) {
                loader = classLoader;
            }
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            Objects.requireNonNull(loader);

            Enumeration<URL> resources = loader.getResources("META-INF/services/" + serviceType);
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()), 256)) {
                    ClassLoader finalLoader = loader;
                    br.lines().filter(s -> s.charAt(0) != '#').map(String::trim).forEach(line -> {
                        try {
                            services.add((S) finalLoader.loadClass(line).newInstance());
                        } catch (Exception e) {
                            e.printStackTrace();// 不需要抛异常，加了条目没有类打个日志即可
                        }
                    });
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return services;
    }

    public static URLClassLoader newURLClassLoader(File[] files, ClassLoader parentLoader) {
        List<URL> urls = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parentLoader);
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
                        copyStream(zip.getInputStream(entry), out);
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

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }

    public static File getServerXml() {
        return getServerXml(getDomain());
    }

    public static File getServerXml(File domain) {
        return Paths.get(domain.getAbsolutePath(), "conf", "qingzhou.xml").toFile();
    }

    public static File getDomain(String domainName) {
        return Paths.get(ServerUtil.getHome().getAbsolutePath(), "domains", domainName).toFile();
    }

    public static File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
    }

    public static File getLib() {
        if (libDir == null) {
            String jarPath = FrameworkContextImpl.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/version";
            int i = jarPath.lastIndexOf(flag);
            int j = jarPath.indexOf("/", i + flag.length());
            libDir = new File(new File(getHome(), "lib"), jarPath.substring(i + 1, j));
        }
        return libDir;
    }

    public static File getDomain() {
        if (domain == null) {
            String domain = System.getProperty("qingzhou.domain");
            if (domain == null || domain.trim().isEmpty()) {
                throw new NullPointerException("qingzhou.domain");
            }
            ServerUtil.domain = new File(domain).getAbsoluteFile();
        }
        return domain;
    }

    public static File getTempDir() {
        return getTempDir(ServerUtil.getDomain());
    }

    public static File getCache(String sub) {
        return getCache(ServerUtil.getDomain(), sub);
    }

    public static File getCache(File domain, String sub) {
        File cacheDir = new File(getTempDir(domain), "cache");
        cacheDir.mkdirs();
        return sub == null ? cacheDir : new File(cacheDir, sub);
    }

    public static File getTempDir(File domain) {
        File tmpdir;
        if (domain != null) {
            tmpdir = new File(domain, "temp");
        } else {
            tmpdir = new File(System.getProperty("java.io.tmpdir"));
        }
        tmpdir.mkdirs();
        return tmpdir;
    }

    // 离线模式：用于离线命令行，只启动微小内核以解析离线命令
    public static boolean isLoadAsOffline() {
        return offlineCommandFile(ServerUtil.getDomain()).exists();
    }

    // 离线模式：用于离线命令行，只启动微小内核以解析离线命令
    public static File offlineCommandFile(File domain) {// SwitchUtil中使用时会读取Lic及console.xml，OfflineCommand使用此方法提前创建此文件
        return getCache(domain, "offline.temp");
    }

    public static boolean isWindows() {
        if (isWindows == null) {
            isWindows = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH).startsWith("win");
        }
        return isWindows;
    }

    public static File getDataXmlFile() {
        if (dataXmlFile == null) {
            dataXmlFile = Paths.get(ServerUtil.getDomain().getAbsolutePath(), "conf", "data.xml").toFile();
        }
        return dataXmlFile;
    }
}
