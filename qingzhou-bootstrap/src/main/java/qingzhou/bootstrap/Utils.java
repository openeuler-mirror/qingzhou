package qingzhou.bootstrap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
            String jarPath = Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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

    public static ClassLoader createClassLoader(File[] jarFiles, ClassLoader parent) {
        URL[] urls = new URL[jarFiles.length];
        try {
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return new URLClassLoader(urls, parent);
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

    public static Map<String, String> getAttributes(File file, String xpath) {
        Document doc = getDocument(file);
        Node node = (Node) evaluate(xpath, doc, XPathConstants.NODE);
        return getAttributes(node);
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
}
