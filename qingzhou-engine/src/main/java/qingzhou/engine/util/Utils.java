package qingzhou.engine.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
    public static boolean isJava9Plus() {
        double javaVerson = Double.parseDouble(System.getProperty("java.specification.version"));
        return javaVerson > 1.8;
    }

    public static void setPropertiesToObj(Object obj, Map<String, String> data) throws Exception {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            setPropertyToObj(obj, entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    public static void setPropertyToObj(Object obj, String key, String val) throws Exception {
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
                Object arg = stringToType(val, parameterTypes[0]);
                writeMethod.invoke(obj, arg);
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

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static Map<String, String> getPropertiesFromObj(Object obj) throws Exception {
        Map<String, String> properties = new HashMap<>();
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        for (PropertyDescriptor p : beanInfo.getPropertyDescriptors()) {
            Method readMethod = p.getReadMethod();
            if (readMethod != null) {
                Object val = readMethod.invoke(obj);
                if (val != null) {
                    if (val instanceof InetAddress) {
                        val = ((InetAddress) val).getHostAddress();
                    } else {
                        Class<?> typeClass = readMethod.getReturnType();
                        if (typeClass != String.class
                                && !isPrimitive(typeClass)) {
                            continue;
                        }
                    }
                    properties.put(p.getName(), String.valueOf(val));
                }
            }
        }
        return properties;
    }

    public static boolean isPrimitive(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        } else return clazz.equals(Boolean.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Short.class);
    }

    private static Set<String> localIps;

    public static Set<String> getLocalIps() {
        if (localIps != null) {
            return localIps;
        }

        localIps = new HashSet<>();

        Set<String> first = new HashSet<>();
        Set<String> second = new HashSet<>();
        Set<String> third = new HashSet<>();
        try {
            OUT:
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback()) {
                    continue;
                }
                String iName = iface.getName().toLowerCase();
                String iDisplayName = iface.getDisplayName().toLowerCase();
                String[] ignores = {"vm", "tun", "vbox", "docker", "virtual"};// *tun* 是 k8s 的 Calico 网络网卡
                for (String ignore : ignores) {
                    if (iDisplayName.contains(ignore)) {
                        continue OUT;
                    }
                }
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();

                    if (inetAddr instanceof Inet6Address) {
                        continue;// for #ITAIT-3712
                    }

                    if (inetAddr != null && !inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            if (iName.startsWith("eth") || iName.startsWith("en") // ITAIT-3024
                            ) {
                                first.add(inetAddr.getHostAddress());
                            } else if (iName.startsWith("wlan")) {
                                second.add(inetAddr.getHostAddress());
                            }
                        } else {
                            third.add(inetAddr.getHostAddress());
                        }
                    }
                }
            }

            if (first.isEmpty()) {
                if (!second.isEmpty()) {
                    first.addAll(second);
                } else {
                    first.addAll(third);
                }
            }

            if (first.isEmpty()) {
                // 如果没有发现 non-loopback地址.只能用最次选的方案
                InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
                if (jdkSuppliedAddress == null) {
                    System.out.println("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
                } else {
                    first.add(jdkSuppliedAddress.getHostAddress());
                }
            }
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Failed to getLocalInetAddress: " + e.getMessage());
        }

        localIps = first;
        if (localIps.isEmpty()) {
            localIps.add("127.0.0.1");
        }
        return localIps;
    }

    public static String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
    }

    public static Collection<String> detectAnnotatedClass(File[] libs, Class<?> annotationClass, String scopePrefix, ClassLoader classLoader) throws Exception {
        Collection<String> targetClasses = new HashSet<>();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassPool classPool;
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
                classPool = new ClassPool(true);
                classPool.appendClassPath(new LoaderClassPath(classLoader));
            } else {
                classPool = ClassPool.getDefault();
                classPool.appendPathList(Arrays.stream(libs).map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
            }
            getScopeClasses(libs, scopePrefix).forEach(s -> {
                try {
                    CtClass ctClass = classPool.get(s);
                    if (ctClass.getAnnotation(annotationClass) != null) {
                        targetClasses.add(s);
                    }
                    ctClass.detach();
                } catch (Exception ignored) {
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return targetClasses;
    }

    private static Collection<String> getScopeClasses(File[] libs, String scopePrefix) throws IOException {
        Collection<String> scopeClasses = new HashSet<>();
        // 找出类名范围
        for (File file : libs) {
            if (!file.getName().endsWith(".jar")) continue;
            try (ZipFile jar = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    String entryName = zipEntry.getName();
                    int i = entryName.indexOf(".class");
                    if (i < 1) continue;
                    String className = entryName.substring(0, i).replace("/", ".");
                    if (scopePrefix != null && !className.startsWith(scopePrefix)) continue;

                    scopeClasses.add(className);
                }
            }
        }
        return scopeClasses;
    }

    public static LinkedHashMap<String, String> streamToProperties(InputStream inputStream) throws Exception {
        LinkedHashMap<String, String> data = new LinkedHashMap<>(); // 保持顺序
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int i = line.indexOf("=");
            if (i != -1) {
                String key = line.substring(0, i);
                String val = line.substring(i + 1);
                data.put(key, val);
            } else {
                data.put(line, "");
            }
        }
        return data;
    }
}
