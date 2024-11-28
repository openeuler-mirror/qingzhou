package qingzhou.engine.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
    public static int getIndex(Object[] objects, Object object) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].equals(object)) {
                return i;
            }
        }
        throw new IllegalArgumentException("not found");
    }

    /**
     * 交换数组中两个元素的位置
     *
     * @param array  待交换元素的数组
     * @param index1 第一个元素的下标
     * @param index2 第二个元素的下标
     */
    public static void swap(Object[] array, int index1, int index2) {
        // 如果入参为空，则返回null
        if (array == null || array.length == 0) {
            return;
        }
        // 如果下标越界，则返回原数组
        if (index1 < 0 || index1 >= array.length || index2 < 0 || index2 >= array.length) {
            return;
        }
        // 交换数组中两个元素的位置
        Object temp = array[index1];
        array[index1] = array[index2];
        array[index2] = temp;
    }

    public static void setPropertiesToObj(Object obj, Map<String, String> parameters) throws Exception {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            setPropertyToObj(obj, entry.getKey(), entry.getValue());
        }
    }

    public static void setPropertyToObj(Object obj, String key, String val) throws Exception {
        if (obj == null || key == null || val == null) return;

        Class<?> objClass = obj.getClass();
        BeanInfo beanInfo = Introspector.getBeanInfo(objClass);
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if (!pd.getName().equals(key)) continue;

            Method writeMethod = pd.getWriteMethod();
            if (writeMethod == null) continue;

            Class<?>[] parameterTypes = writeMethod.getParameterTypes();
            if (parameterTypes.length == 1) {
                Object arg = convert(parameterTypes[0], val);
                writeMethod.invoke(obj, arg);
                return;
            }
        }

        // 类没有 getter setter，使用反射设值
        Field field = objClass.getDeclaredField(key);
        field.setAccessible(true);
        Class<?> fieldType = field.getType();
        if (fieldType == boolean.class || fieldType == Boolean.class) field.set(obj, Boolean.valueOf(val));
        else if (fieldType == byte.class || fieldType == Byte.class) field.set(obj, Byte.valueOf(val));
        else if (fieldType == short.class || fieldType == Short.class) field.set(obj, Short.valueOf(val));
        else if (fieldType == int.class || fieldType == Integer.class) field.set(obj, Integer.valueOf(val));
        else if (fieldType == long.class || fieldType == Long.class) field.set(obj, Long.valueOf(val));
        else if (fieldType == float.class || fieldType == Float.class) field.set(obj, Float.valueOf(val));
        else if (fieldType == double.class || fieldType == Double.class) field.set(obj, Double.valueOf(val));
        else if (fieldType == char.class || fieldType == Character.class) field.set(obj, val.charAt(0));
        else field.set(obj, val);
    }

    public static Object convert(Class<?> type, String value) throws Exception {
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
            if (Utils.isBlank(value)) {
                return null; // value=“” 时，会报转化类型异常。
            }
            return InetAddress.getByName(value);
        }

        if (type.equals(int.class) || type.equals(Integer.class)) {
            if (Utils.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Integer.parseInt(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            if (Utils.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Long.parseLong(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            if (Utils.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Float.parseFloat(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            if (Utils.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Double.parseDouble(value);
        }

        throw new IllegalArgumentException();
    }

    public static <T> T doInThreadContextClassLoader(ClassLoader useLoader, InvokeInThreadContextClassLoader<T> invoker) throws Exception {
        if (useLoader == null) return invoker.invoke();

        ClassLoader originLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(useLoader);
            return invoker.invoke();
        } finally {
            Thread.currentThread().setContextClassLoader(originLoader);
        }
    }

    public interface InvokeInThreadContextClassLoader<T> {
        T invoke() throws Exception;
    }

    public static boolean contains(Object[] array, Object obj) {
        if (array == null) {
            return false;
        }
        for (Object o : array) {
            if (Objects.equals(o, obj)) {
                return true;
            }
        }
        return false;
    }

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    public static Throwable getCause(Throwable e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause.getCause() == null || cause.getCause() == cause) {
                return cause;
            }
            cause = cause.getCause();
        }

        return e;
    }

    public static String exceptionToString(Throwable e) {
        String stackTrace = stackTraceToString(e.getStackTrace());
        return e.getMessage() + "\t" + stackTrace;
    }

    public static String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
    }

    public static Collection<String> detectAnnotatedClass(File[] libs, Class<?> annotationClass, ClassLoader classLoader) throws Exception {
        return Utils.doInThreadContextClassLoader(classLoader, () -> {
            Collection<String> targetClasses = new HashSet<>();
            ClassPool classPool;
            if (classLoader != null) {
                classPool = new ClassPool(true);
                classPool.appendClassPath(new LoaderClassPath(classLoader));
            } else {
                classPool = ClassPool.getDefault();
                classPool.appendPathList(Arrays.stream(libs).map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
            }
            getScopeClasses(libs).forEach(s -> {
                try {
                    CtClass ctClass = classPool.get(s);
                    if (ctClass.getAnnotation(annotationClass) != null) {
                        targetClasses.add(s);
                    }
                    ctClass.detach();
                } catch (Exception ignored) {
                }
            });
            return targetClasses;
        });
    }

    private static Collection<String> getScopeClasses(File[] libs) throws IOException {
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
                    scopeClasses.add(className);
                }
            }
        }
        return scopeClasses;
    }

    public static Properties zipEntryToProperties(File file, String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(file, ZipFile.OPEN_READ)) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = zip.getInputStream(entry)) {
                return streamToProperties(inputStream);
            }
        }
    }

    public static Properties streamToProperties(InputStream inputStream) throws Exception {
        Properties data = new Properties(); // 保持顺序
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

    public static Map<String, String> stringToMap(String str, String SP) {
        Map<String, String> map = new LinkedHashMap<>();
        if (Utils.isBlank(str)) {
            return map;
        }
        String[] envArr = str.split(SP);
        for (String env : envArr) {
            int i = env.indexOf("=");
            if (i < 0) {
                map.put(env, "");
            } else {
                String name = env.substring(0, i);
                String value = env.substring(i + 1);
                map.put(name, value);
            }
        }
        return map;
    }

    public static String toUrl(Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return url.substring(0, url.length() - 1);
    }
}
