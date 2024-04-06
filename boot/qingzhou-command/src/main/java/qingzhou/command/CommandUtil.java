package qingzhou.command;

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
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommandUtil {
    private static Boolean isWindows;
    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    public static File getLibDir() {
        String jarPath = Admin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String flag = "/command/qingzhou-command.jar";
        int i = jarPath.indexOf(flag);
        String pre = jarPath.substring(0, i);
        return new File(pre);
    }

    public static void log(String msg) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >>> ");
        String logPrefix = dateFormat.format(new Date());
        System.out.println(logPrefix + msg);
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
                Object arg = CommandUtil.stringToType(val, parameterTypes[0]);
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
        if (CommandUtil.hasCmdInjectionRisk(javaCmd)) { // fix #ITAIT-4940
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
            CommandUtil.readInputStreamWithThread(start.getInputStream(), collectorUtil, "stdout");
            CommandUtil.readInputStreamWithThread(start.getErrorStream(), collectorUtil, "stderr");
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

    public static void readInputStreamWithThread(InputStream inputStream, CommandUtil.StringCollector output, String name) {
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
        final CommandUtil.StringCollector output;

        public StreamConsumer(InputStream inputStream, String type, CommandUtil.StringCollector output) {
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
