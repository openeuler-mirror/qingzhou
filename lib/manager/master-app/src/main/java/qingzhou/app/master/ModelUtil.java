package qingzhou.app.master;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;

import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.engine.util.Utils;

public class ModelUtil {
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
                        Class<?> typeClass = readMethod.getReturnType();
                        if (typeClass != String.class
                                && !Utils.isPrimitive(typeClass)) {
                            continue;
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

    public static boolean query(Map<String, String> query, Supplier supplier) {
        if (query == null) return true;

        for (Map.Entry<String, String> e : query.entrySet()) {
            String queryField = e.getKey();
            String queryValue = e.getValue();

            Map<String, String> data = supplier.get();
            String val = data.get(queryField);
            if (val == null) return false;

            AppInfo appInfo = Main.getService(Deployer.class).getApp(DeployerConstants.APP_MASTER).getAppInfo();
            ModelFieldInfo fieldInfo = appInfo.getModelInfo(supplier.getModelName()).getModelFieldInfo(queryField);
            String querySP = fieldInfo.getSeparator();
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

    public static List<String[]> listData(String[] allIds, IdSupplier idSupplier,
                                          int pageNum, int pageSize, String[] fieldNames) throws IOException {
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        List<String[]> data = new ArrayList<>();
        for (String id : subList) {
            String[] result = new String[fieldNames.length];
            Map<String, String> idData = idSupplier.get(id);
            for (int i = 0; i < fieldNames.length; i++) {
                result[i] = idData.get(fieldNames[i]);
            }
            data.add(result);
        }
        return data;
    }

    public interface IdSupplier {
        Map<String, String> get(String id) throws IOException;
    }

    public interface Supplier {
        String getModelName();

        Map<String, String> get();
    }
}
