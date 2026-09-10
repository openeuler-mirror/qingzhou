package qingzhou.config.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * qingzhou.properties 文本解析：只去左侧空白、# 为注释、单独反斜杠视为换行、行尾反斜杠折行。
 * 本地配置解析与远程 pid 文档解析共用，避免两份实现漂移。
 */
public final class ConfigText {
    private ConfigText() {
    }

    public static Map<String, String> parse(String text) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        StringBuilder folded = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            for (String line; (line = reader.readLine()) != null; ) {
                line = line.replaceAll("^[\\s\u3000]+", "");
                if (line.isEmpty() || line.startsWith("#") || line.equals("\\")) continue;

                if (line.endsWith("\\")) {// 折行
                    folded.append(line, 0, line.length() - 1);
                    continue;
                }
                String target = folded.append(line).toString();
                folded.setLength(0);

                int i = target.indexOf('=');
                String key = (i > 0 ? target.substring(0, i) : target).trim();
                if (!key.isEmpty()) result.put(key, i > 0 ? target.substring(i + 1).trim() : "");
            }
        }
        return result;
    }
}
