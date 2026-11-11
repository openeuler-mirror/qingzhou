package qingzhou.config.remote.etcd;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * etcd REST 报文的最小 JSON 读写：qingzhou-config bundle 只依赖 JDK，框架 Json 组件（jackson）
 * 封装在 qingzhou-json bundle 内且未导出，故自带实现，只覆盖报文用到的对象、数组与字符串。
 */
final class EtcdJson {
    private final String json;
    private int pos;

    private EtcdJson(String json) {
        this.json = json;
    }

    static String toJson(Object src) throws Exception {
        StringBuilder sb = new StringBuilder("{");
        for (Field field : src.getClass().getFields()) {
            Object value = field.get(src);
            if (value != null) { // 空值与 etcd 侧默认值等价，不必发送
                sb.append(sb.length() > 1 ? "," : "").append('"').append(field.getName()).append("\":\"")
                        .append(escape(value.toString())).append('"');
            }
        }
        return sb.append('}').toString();
    }

    static <T> T fromJson(String json, Class<T> classOfT) throws Exception {
        T target = classOfT.getDeclaredConstructor().newInstance();
        Map<?, ?> tree = (Map<?, ?>) new EtcdJson(json).value();
        for (Field field : classOfT.getFields()) {
            Object value = bind(tree.get(field.getName()), field);
            if (value != null) field.set(target, value);
        }
        return target;
    }

    // 只按 DTO 声明的字段装配，响应中其余字段（header、count 等）一律忽略
    private static Object bind(Object node, Field field) throws Exception {
        if (node == null) return null;
        if (node instanceof List) {
            Class<?> itemType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            List<Object> items = new ArrayList<>();
            for (Object item : (List<?>) node) items.add(bind(item, itemType));
            return items;
        }
        return bind(node, field.getType());
    }

    private static Object bind(Object node, Class<?> type) throws Exception {
        if (type.isInstance(node)) return node;
        Object target = type.getDeclaredConstructor().newInstance();
        Map<?, ?> map = (Map<?, ?>) node;
        for (Field field : type.getFields()) {
            Object value = bind(map.get(field.getName()), field);
            if (value != null) field.set(target, value);
        }
        return target;
    }

    private Object value() {
        char c = json.charAt(skip());
        if (c == '{') {
            Map<String, Object> map = new HashMap<>();
            for (pos++; json.charAt(skip()) != '}'; ) {
                String key = string();
                pos = skip() + 1; // 冒号
                map.put(key, value());
                if (json.charAt(skip()) == ',') pos++;
            }
            pos++;
            return map;
        }
        if (c == '[') {
            List<Object> list = new ArrayList<>();
            for (pos++; json.charAt(skip()) != ']'; ) {
                list.add(value());
                if (json.charAt(skip()) == ',') pos++;
            }
            pos++;
            return list;
        }
        if (c == '"') return string();

        int start = pos;
        while (pos < json.length() && ",}]".indexOf(json.charAt(pos)) < 0) pos++;
        String literal = json.substring(start, pos).trim(); // 数字/布尔/null：DTO 不消费，原样保留
        return "null".equals(literal) ? null : literal;
    }

    private String string() {
        StringBuilder sb = new StringBuilder();
        for (pos++; ; ) {
            char c = json.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char escaped = json.charAt(pos++);
            switch (escaped) {
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u': sb.append((char) Integer.parseInt(json.substring(pos, pos += 4), 16)); break;
                default: sb.append(escaped); // \" \\ \/ 等取字面量
            }
        }
    }

    private int skip() {
        while (pos < json.length() && json.charAt(pos) <= ' ') pos++;
        return pos;
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
