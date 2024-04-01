package qingzhou.bootstrap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qingzhou.bootstrap.launcher.Admin;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Utils {
    private static File home;
    private static File libDir;
    private static File domain;
    private static Boolean isWindows;
    private static final XPath xpath = XPathFactory.newInstance().newXPath();

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

    public static boolean isPortOpened(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(host), port), 1000);// 如果超时时间太长，会导致创建域的页面卡顿！！
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 删除 文件夹
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

    public static File getLibDir() {
        if (libDir == null) {
            String jarPath = Admin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String flag = "/version";
            int i = jarPath.lastIndexOf(flag);
            int j = jarPath.indexOf("/", i + flag.length());
            libDir = new File(new File(getHome(), "lib"), jarPath.substring(i + 1, j));
        }
        return libDir;
    }

    public static File getHome() {
        if (home == null) {
            home = new File(System.getProperty("qingzhou.home"));
        }
        return home;
    }

    public static File getDomain(String domainName) {
        return newFile(getHome(), "domains", domainName);
    }

    public static File getDomain() {
        if (domain == null) {
            String domain = System.getProperty("qingzhou.domain");
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException();// 不要在这里设置 domain1，应该在调用端去捕捉异常并处理
            }
            try {
                Utils.domain = new File(domain).getCanonicalFile();
            } catch (IOException e) {
                throw new IllegalArgumentException();// 不要在这里设置 domain1，应该在调用端去捕捉异常并处理
            }
        }
        return domain;
    }

    public static File getServerXml(File domain) {
        return Utils.newFile(domain, "conf", "server.xml");
    }

    public static boolean isWindows() {
        if (isWindows == null) {
            isWindows = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH).startsWith("win");
        }
        return isWindows;
    }

    public static void setObjectValues(Object obj, Map<String, String> map) throws Exception {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            setObjectValue(obj, entry.getKey(), entry.getValue());
        }
    }

    public static void setObjectValue(Object obj, String key, String val) throws Exception {
        if (obj == null || key == null || val == null) {
            return;
        }

        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if (!pd.getName().equals(key)) {
                continue;
            }
            Method writeMethod = pd.getWriteMethod();
            if (writeMethod == null) {
                continue;
            }

            Class<?>[] parameterTypes = writeMethod.getParameterTypes();
            if (parameterTypes.length == 1) {
                Object arg = Utils.stringToType(val, parameterTypes[0]);
                writeMethod.invoke(obj, arg);
                return;
            } else {
                throw new IllegalArgumentException("parameter types error");
            }
        }
    }


    public static Object stringToType(String value, Class<?> type) throws Exception {
        if (value == null) {
            return null;
        }

        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }

        if (type == InetAddress.class) {
            if (isBlank(value)) {
                return null; // value=“” 时，会报转化类型异常。
            }
            return InetAddress.getByName(value);
        }

        if (type.equals(int.class) || type.equals(Integer.class)) {
            if (isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Integer.parseInt(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            if (isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Long.parseLong(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            if (isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Float.parseFloat(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            if (isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Double.parseDouble(value);
        }

        // 其它类型是不支持的
        // throw new IllegalArgumentException();
        return null;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

    public static String join(final Iterable<?> iterable, final String separator) {
        if (iterable == null) {
            return null;
        }
        return join(iterable.iterator(), separator);
    }

    public static String join(final Iterator<?> iterator, final String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return Objects.toString(first, "");
        }

        // two or more elements
        final StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
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

    public static int parseJavaVersion(String ver) {
        try {
            if (ver.startsWith("1.")) {
                ver = ver.substring(2);
            }
            int firstVer = ver.indexOf(".");
            if (firstVer > 0) {
                ver = ver.substring(0, firstVer);
            }
            return Integer.parseInt(ver);
        } catch (Exception e) {
            return 8;
        }
    }

    public static boolean hasCmdInjectionRisk(String arg) {
        for (String f : new String[]{"`"}) {// 命令行执行注入漏洞
            if (arg.contains(f)) {
                return true;
            }
        }

        return false;
    }

    public static String getJavaVersionString(String javaCmd) throws IOException {
        if (Utils.hasCmdInjectionRisk(javaCmd)) { // fix #ITAIT-4940
            throw new IllegalArgumentException("This command may have security risks: " + javaCmd);
        }
        String[] javaVersion = {null};
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(javaCmd);
        builder.command().add("-version");
        Process start = builder.start();
        StringCollector collectorUtil = null;
        try {
            collectorUtil = new StringCollector() {
                @Override
                public void collect(String line) {
                    super.collect(line);
                    String versionFlag = "version \"";
                    int i = line.toLowerCase().indexOf(versionFlag);
                    if (i != -1) {
                        line = line.substring(i + versionFlag.length());
                        int endFlag = line.indexOf("\"");
                        if (javaVersion[0] == null) {
                            javaVersion[0] = line.substring(0, endFlag);
                        }
                    }
                }
            };
            Utils.readInputStreamWithThread(start.getInputStream(), collectorUtil, "stdout");
            Utils.readInputStreamWithThread(start.getErrorStream(), collectorUtil, "stderr");
        } finally {
            try {
                start.waitFor();
            } catch (Exception ignored) {
            }
            try {
                start.destroyForcibly();
            } catch (Exception ignored) {
            }
            if (collectorUtil != null) {
                collectorUtil.finish();
            }
        }
        return javaVersion[0];
    }

    public static String stripQuotes(String value) {
        if (isBlank(value))
            return value;

        while (value.startsWith("\"")) {
            value = value.substring(1);
        }
        while (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static File getTemp(File domain, String sub) {
        File tmpdir;
        if (domain != null) {
            tmpdir = new File(domain, "temp");
        } else {
            tmpdir = new File(System.getProperty("java.io.tmpdir"));
        }
        mkdirs(tmpdir);
        return sub == null ? tmpdir : new File(tmpdir, sub);
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

    public static <S> List<S> loadServices(String serviceType, ClassLoader classLoader) throws Exception {
        List<S> services = new ArrayList<>();

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
                        throw new RuntimeException(e);
                    }
                });
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        return services;
    }

    public static void readInputStreamWithThread(InputStream inputStream, Utils.StringCollector output, String name) {
        new Thread(new StreamConsumer(inputStream, name, output), "QZ-InputStream-Collector").start();
    }

    public static Map<String, String> getAttributes(File file, String xpath) {
        Document doc = getDocument(file);
        Node node = (Node) evaluate(xpath, doc, XPathConstants.NODE);
        return getAttributes(node);
    }

    public static List<Map<String, String>> getAttributesList(File file, String xpath) {
        Document doc = getDocument(file);
        return getAttributesList(doc, xpath);
    }

    public static List<Map<String, String>> getAttributesList(Document doc, String xpath) {
        NodeList nodeList = (NodeList) evaluate(xpath, doc, XPathConstants.NODESET);
        if (nodeList == null || nodeList.getLength() == 0) {
            return null;
        }

        List<Map<String, String>> list = new ArrayList<>();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Node item = nodeList.item(i);
            Map<String, String> properties = getAttributes(item);
            list.add(properties);
        }

        return list;
    }

    public static Map<String, String> getAttributes(Node node) {
        if (node == null) {
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        NamedNodeMap namedNodeMap = node.getAttributes();
        int length = namedNodeMap.getLength();
        for (int i = 0; i < length; i++) {
            Node item = namedNodeMap.item(i);
            properties.put(item.getNodeName(), item.getNodeValue());
        }
        return properties;
    }

    private static Object evaluate(String expression, Object item, QName returnType) {
        if (expression.contains(" or ") || expression.contains(" not ") || expression.contains("\"")) { // 预防 Xpath 注入漏洞
            throw new IllegalArgumentException(expression);
        }

        try {
            return xpath.evaluate(expression, item, returnType);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private static Document getDocument(File file) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(
                    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",// 解决设置 -D参数冲突问题等
                    Thread.currentThread().getContextClassLoader());
            DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream inputStream = new BufferedInputStream(Objects.requireNonNull(Files.newInputStream(file.toPath())))) {
                return db.parse(inputStream);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static class StringCollector {
        private StringBuilder data = new StringBuilder();

        public synchronized void collect(String content) {
            if (data != null) {
                data.append(content);
            }
        }

        public synchronized void finish() {
            data = null;
        }
    }

    private static class StreamConsumer implements Runnable {
        final InputStream is;
        final String type;
        final Utils.StringCollector output;

        public StreamConsumer(InputStream inputStream, String type, Utils.StringCollector output) {
            this.is = inputStream;
            this.type = type;
            this.output = output;
        }

        /**
         * Runs this object as a separate thread, printing the contents of the InputStream
         * supplied during instantiation, to either stdout or stderr
         */
        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (output != null) {
                        String msg = type + ">" + line + System.lineSeparator();
                        output.collect(msg);
                    }
                }
            } catch (Exception e) {
                if (output != null) {
                    String msg = type + ">" + e.getMessage() + System.lineSeparator();
                    output.collect(msg);
                }
            }
        }
    }
}
