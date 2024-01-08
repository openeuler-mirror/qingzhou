package qingzhou.framework.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ObjectUtil {
    public static String inputStreamToString(InputStream is, Charset cs) throws IOException {
        List<String> content = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, cs != null ? cs : StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            content.add(line);
        }
        return StringUtil.join(content, System.lineSeparator());
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

    public static Properties map2Properties(Map<String, String> attributes) {
        Properties properties = new Properties();
        properties.putAll(attributes);
        return properties;
    }

    public static Map<String, String> properties2Map(Properties properties) {
        Map<String, String> map = new HashMap<>();
        properties.forEach((key, value) -> map.put(
                String.valueOf(key), String.valueOf(value)));
        return map;
    }

    public static boolean isSameMap(Map<String, String> oldMap, Map<String, String> newMap) {
        if (oldMap == null || newMap == null) {
            return false;
        }

        if (oldMap.size() != newMap.size()) {
            return false;
        }

        for (String k : oldMap.keySet()) {
            String oldVal = oldMap.get(k);
            String newVal = newMap.get(k);
            if (!Objects.equals(oldVal, newVal)) {
                return false;
            }
        }
        return true;
    }

    public static Object getObjectValue(Object obj, String field) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if (pd.getName().equals(field)) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    return readMethod.invoke(obj);
                }
            }
        }

        Field f = getField(obj.getClass(), field);
        if (f != null) {
            return f.get(obj);
        }

        throw ExceptionUtil.unexpectedException();
    }

    public static Field getField(Class<?> objClass, String field) {
        try {
            Field f = objClass.getDeclaredField(field);
            if (!Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
                return f;
            }
        } catch (NoSuchFieldException ignored) {
        }

        return null;
    }

    public static List<String> getClassFields(Class<?> clazz) {
        List<String> fields = new ArrayList<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getReadMethod() == null) {
                    continue;
                }
                if (pd.getWriteMethod() == null) {
                    continue;
                }

                fields.add(pd.getName());
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return fields;
    }

    public static Number convertNumber(Class<?> type, String value) {
        if (StringUtil.isBlank(value)) {
            return 0; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
        }
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.valueOf(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.valueOf(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.valueOf(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.valueOf(value);
        }

        throw new IllegalArgumentException();
    }

    public static Object convert(Class<?> type, String value) throws Exception {
        if (type.equals(String.class)) {
            return value;
        }

        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        }

        if (type == InetAddress.class) {
            if (StringUtil.isBlank(value)) {
                return null; // value=“” 时，会报转化类型异常。
            }
            return InetAddress.getByName(value);
        }

        return convertNumber(type, value);
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
                Object arg = StringUtil.stringToType(val, parameterTypes[0]);
                writeMethod.invoke(obj, arg);
                return;
            } else {
                throw new IllegalArgumentException("parameter types error");
            }
        }
    }

    private ObjectUtil() {
    }
}
