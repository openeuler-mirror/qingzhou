package com.tongtech.zookeeper.starter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Util {

    /** 3.5.0 版本号的分段表示，避免每次比较时重复 split */
    private static final String[] VERSION_3_5_0 = "3.5.0".split("\\.");

    private Util() {
    }

    /** 判断版本号是否不低于 3.5.0 */
    public static boolean comparativeVersion(String version) {
        if (isBlank(version)) {
            return false;
        }
        String[] parts = version.split("-")[0].split("\\.");
        for (int i = 0; i < parts.length && i < VERSION_3_5_0.length; i++) {
            int current = Integer.parseInt(parts[i]);
            int base = Integer.parseInt(VERSION_3_5_0[i]);
            if (current > base) {
                return true;
            } else if (current < base) {
                return false;
            }
        }
        return true;
    }

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static Map<String, String> prop2Map(Properties properties) {
        Map<String, String> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
    }
}
