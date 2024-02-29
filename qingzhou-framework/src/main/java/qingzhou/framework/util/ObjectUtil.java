package qingzhou.framework.util;

import qingzhou.bootstrap.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectUtil {
    public static String inputStreamToString(InputStream is, Charset cs) throws IOException {
        List<String> content = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, cs != null ? cs : StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            content.add(line);
        }
        return String.join(System.lineSeparator(), content);
    }

    public static Properties streamToProperties(InputStream inputStream) throws Exception {
        return Utils.streamToProperties(inputStream);
    }

    public static Properties map2Properties(Map<String, String> attributes) {
        Properties properties = new Properties();
        properties.putAll(attributes);
        return properties;
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

    private ObjectUtil() {
    }
}
