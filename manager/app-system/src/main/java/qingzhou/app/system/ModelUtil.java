package qingzhou.app.system;

import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;

public class ModelUtil {
    private static final String STRING_PROPERTIES_SP = DeployerConstants.DEFAULT_DATA_SEPARATOR;

    public static void setPropertiesToObj(Object obj, Map<String, String> data) throws Exception {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            setPropertyToObj(obj, entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private static void setPropertyToObj(Object obj, String key, String val) throws Exception {
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

    private static Object stringToType(String value, Class<?> type) throws Exception {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }

        if (type == InetAddress.class) {
            // value=“” 时，会报转化类型异常。
            return InetAddress.getByName(value);
        }

        if (type == Properties.class) {
            Properties properties = new Properties();
            for (String kv : value.split(STRING_PROPERTIES_SP)) {
                int i = kv.indexOf("=");
                if (i > 0) {
                    properties.setProperty(kv.substring(0, i), kv.substring(i + 1));
                }
            }
            return properties;
        }

        if (type.equals(int.class) || type.equals(Integer.class)) {
            // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            return Integer.parseInt(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            return Long.parseLong(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            return Float.parseFloat(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            return Double.parseDouble(value);
        }

        // 其它类型是不支持的
        // throw new IllegalArgumentException();
        return null;
    }

    public static Map<String, String> getPropertiesFromObj(Object obj) {
        Map<String, String> properties = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            for (PropertyDescriptor p : beanInfo.getPropertyDescriptors()) {
                Method readMethod = p.getReadMethod();
                if (readMethod != null) {
                    Object val = readMethod.invoke(obj);
                    if (val != null) {
                        if (val instanceof InetAddress) {
                            val = ((InetAddress) val).getHostAddress();
                        }
                        if (val instanceof Properties) {
                            val = propertiesToString((Properties) val);
                        } else {
                            Class<?> typeClass = readMethod.getReturnType();
                            if (typeClass != String.class
                                    && !Utils.isPrimitive(typeClass)) {
                                continue;
                            }
                        }
                        properties.put(p.getName(), String.valueOf(val));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static String propertiesToString(Properties properties) {
        if (properties == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append(STRING_PROPERTIES_SP);
        }
        return sb.toString();
    }

    public static boolean query(Map<String, String> query, Supplier supplier) {
        if (query == null) return true;

        for (Map.Entry<String, String> e : query.entrySet()) {
            String queryField = e.getKey();
            String queryValue = e.getValue();

            Map<String, String> data = supplier.get();
            String val = data.get(queryField);

            String querySP = supplier.getFieldSeparator(queryField);
            if (queryValue.contains(querySP)) {
                boolean found = false;
                for (String q : queryValue.split(querySP)) {
                    if (val.equals(q)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            } else {
                if (!val.contains(queryValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Map<String, String>> listData(String[] allIds, IdSupplier idSupplier,
                                                     int pageNum, int pageSize, String[] fieldNames) throws IOException {
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        for (String id : subList) {
            Map<String, String> result = new HashMap<>();

            Map<String, String> idData = idSupplier.get(id);
            for (String fieldName : fieldNames) {
                result.put(fieldName, idData.get(fieldName));
            }

            data.add(result);
        }
        return data;
    }

    public interface IdSupplier {
        Map<String, String> get(String id) throws IOException;
    }

    public interface Supplier {
        String getFieldSeparator(String field);

        Map<String, String> get();
    }
}
