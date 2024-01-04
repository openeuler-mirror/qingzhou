package qingzhou.framework.util;

import qingzhou.framework.AppInfo;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Constants;
import qingzhou.framework.impl.FrameworkContextImpl;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

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
import java.text.DecimalFormat;
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

public class ServerUtil { // todo：将无状态的工具方法，拆分到对应的 util/*Utils类里面
    public static final String remoteKeyName = "remoteKey";
    public static final String localKeyName = "localKey";
    public static final String publicKeyName = "publicKey";
    public static final String privateKeyName = "privateKey";
    public static final String remotePublicKeyName = "remotePublicKey";

    private static Boolean isWindows;
    private static File domain;
    private static File dataXmlFile;
    private static File libDir;
    private static File home;

    public static Logger getLogger() {
        return getFrameworkContext().getService(LoggerService.class).getLogger();
    }

    public static FrameworkContext getFrameworkContext() {
        return FrameworkContextImpl.getFrameworkContext();
    }

    public static AppContext getMasterAppContext() {
        AppInfo appInfo = ServerUtil.getFrameworkContext().getAppManager().getAppInfo(Constants.MASTER_APP_NAME);
        return appInfo.getAppContext();
    }

    public static ConsoleContext getMasterConsoleContext() {
        return ServerUtil.getMasterAppContext().getConsoleContext();
    }

    public static String maskMBytes(long val) {
        double v = ((double) val) / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    public static boolean isBlank(String value) {
        return value == null || "".equals(value.trim());
    }

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

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
        return Paths.get(domain.getAbsolutePath(), "conf", "server.xml").toFile();
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


    public static File getApps() {
        return FileUtil.newFile(getDomain(), "apps");
    }

    public static synchronized File getSecureFile(File domain) throws IOException {
        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        File secureFile = FileUtil.newFile(secureDir, "secure");
        if (!secureFile.exists()) {
            if (!secureFile.createNewFile()) {
                throw ExceptionUtil.unexpectedException(secureFile.getPath());
            }
        }

        return secureFile;
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

    public static boolean isEffective(FieldValueRetriever retriever, String effectiveWhen) throws Exception {
        if (StringUtil.isBlank(effectiveWhen)) {
            return true;
        }

        AndOrQueue queue = null;
        String[] split;
        if ((split = effectiveWhen.split("&")).length > 1) {
            queue = new AndOrQueue(true);
        } else if ((split = effectiveWhen.split("\\|")).length > 1) {
            queue = new AndOrQueue(false);
        }
        if (queue == null) {
            if (split.length > 0) {
                queue = new AndOrQueue(true);
            }
        }
        if (queue == null) {
            return true;
        }

        String notEqStr = "!=";
        String eqStr = "=";
        for (String s : split) {
            int notEq = s.indexOf(notEqStr);
            if (notEq > 1) {
                String f = s.substring(0, notEq);
                String v = s.substring(notEq + notEqStr.length());
                queue.addComparator(new Comparator(false, retriever.getFieldValue(f), v));
                continue;
            }
            int eq = s.indexOf(eqStr);
            if (eq > 1) {
                String f = s.substring(0, eq);
                String v = s.substring(eq + eqStr.length());
                queue.addComparator(new Comparator(true, retriever.getFieldValue(f), v));
            }
        }

        return queue.compare();
    }

    private static final class Comparator {
        final boolean eqOrNot;
        final String v1;
        final String v2;

        Comparator(boolean eqOrNot, String v1, String v2) {
            this.eqOrNot = eqOrNot;
            this.v1 = v1;
            this.v2 = v2;
        }

        boolean compare() {
            String vv1 = v1;
            String vv2 = v2;
            if (vv1 != null) {
                vv1 = vv1.toLowerCase();
            }
            if (vv2 != null) {
                vv2 = vv2.toLowerCase();
            }
            return eqOrNot == Objects.equals(vv1, vv2);
        }
    }

    private static final class AndOrQueue {
        final boolean andOr;
        final List<Comparator> comparators = new ArrayList<>();

        AndOrQueue(boolean andOr) {
            this.andOr = andOr;
        }

        void addComparator(Comparator comparator) {
            comparators.add(comparator);
        }

        boolean compare() {
            if (andOr) {
                for (Comparator c : comparators) {
                    if (!c.compare()) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Comparator c : comparators) {
                    if (c.compare()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public interface FieldValueRetriever {
        String getFieldValue(String fieldName) throws Exception;
    }

}
