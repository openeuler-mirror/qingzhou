package qingzhou.framework.util;

import qingzhou.framework.util.pattern.Callback;

import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static Object stringToType(String value, Class<?> type) throws Exception {
        if (value == null) {
            return null;
        }

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

        if (type.equals(int.class) || type.equals(Integer.class)) {
            if (StringUtil.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Integer.valueOf(value);
        }
        if (type.equals(long.class) || type.equals(Long.class)) {
            if (StringUtil.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Long.valueOf(value);
        }
        if (type.equals(float.class) || type.equals(Float.class)) {
            if (StringUtil.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Float.valueOf(value);
        }
        if (type.equals(double.class) || type.equals(Double.class)) {
            if (StringUtil.isBlank(value)) {
                return null; // 如果字符串转化数字时，value=“” 时，会报转化类型异常。
            }
            return Double.valueOf(value);
        }

        // 其它类型是不支持的
        // throw new IllegalArgumentException();
        return null;
    }

    public static String propertiesToString(Properties properties) {
        if (properties == null) {
            return "";
        }
        final Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        List<String> list = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries) {
            list.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(System.lineSeparator(), list);
    }

    public static String mapToString(Map<String, String> map) {
        Objects.requireNonNull(map);
        if (map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static Map<String, String> stringToMap(String str) throws Exception {
        return stringToMap(str, null);
    }

    public static Map<String, String> stringToMap(String str, Callback<String, String> callback) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtil.isBlank(str)) {
            return map;
        }
        String[] envArr = str.split(",");
        for (String env : envArr) {
            int i = env.indexOf("=");
            if (i < 0) {
                map.put(env, "");
            } else {
                String name = env.substring(0, i);
                String value = env.substring(i + 1);
                if (callback != null) {
                    value = callback.run(value);
                }
                map.put(name, value);
            }
        }
        return map;
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

    /**
     * 是否包含3个及以上相同或字典连续字符
     */
    public static boolean isContinuousChar(String password) {
        char[] chars = password.toCharArray();
        for (int i = 0; i < chars.length - 2; i++) {
            int n1 = chars[i];
            int n2 = chars[i + 1];
            int n3 = chars[i + 2];
            // 判断重复字符
            if (n1 == n2 && n1 == n3) {
                return true;
            }
            // 判断连续字符： 正序 + 倒序
            if ((n1 + 1 == n2 && n1 + 2 == n3) || (n1 - 1 == n2 && n1 - 2 == n3)) {
                return true;
            }
        }
        return false;
    }

    private StringUtil() {
    }
}
