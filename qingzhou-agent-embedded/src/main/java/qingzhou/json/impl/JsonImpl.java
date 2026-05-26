package qingzhou.json.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qingzhou.json.Json;

/**
 * Pure Java JSON implementation using reflection (no Jackson/Gson dependency).
 */
public class JsonImpl implements Json {

    @Override
    public String toJson(Object src) throws Exception {
        if (src == null) return "null";
        StringBuilder sb = new StringBuilder();
        writeValue(sb, src);
        return sb.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, Class<T> classOfT) throws Exception {
        if (json == null || json.trim().isEmpty()) return null;
        Tokenizer tokenizer = new Tokenizer(json.trim());
        Object value = parseValue(tokenizer);
        return (T) convert(value, classOfT);
    }

    private void writeValue(StringBuilder sb, Object value) throws Exception {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Character) {
            writeString(sb, value.toString());
        } else if (value instanceof Map) {
            writeMap(sb, (Map<?, ?>) value);
        } else if (value instanceof Collection) {
            writeCollection(sb, (Collection<?>) value);
        } else if (value.getClass().isArray()) {
            writeArray(sb, value);
        } else if (value instanceof Enum) {
            writeString(sb, ((Enum<?>) value).name());
        } else {
            writeObject(sb, value);
        }
    }

    private void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private void writeMap(StringBuilder sb, Map<?, ?> map) throws Exception {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private void writeCollection(StringBuilder sb, Collection<?> col) throws Exception {
        sb.append('[');
        boolean first = true;
        for (Object item : col) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private void writeArray(StringBuilder sb, Object arr) throws Exception {
        sb.append('[');
        int len = Array.getLength(arr);
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, Array.get(arr, i));
        }
        sb.append(']');
    }

    private void writeObject(StringBuilder sb, Object obj) throws Exception {
        sb.append('{');
        Field[] fields = obj.getClass().getFields();
        boolean first = true;
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;
            if (!first) sb.append(',');
            first = false;
            writeString(sb, field.getName());
            sb.append(':');
            writeValue(sb, field.get(obj));
        }
        sb.append('}');
    }

    // --- Tokenizer ---

    private static class Tokenizer {
        private final String json;
        private int pos;

        Tokenizer(String json) {
            this.json = json;
            this.pos = 0;
        }

        char peek() {
            skipWhitespace();
            return pos < json.length() ? json.charAt(pos) : 0;
        }

        char next() {
            skipWhitespace();
            return json.charAt(pos++);
        }

        String nextToken() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '"') return readString();
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                pos++;
                return String.valueOf(c);
            }
            return readLiteral();
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private String readString() {
            StringBuilder sb = new StringBuilder();
            pos++; // skip opening "
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char next = json.charAt(pos++);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = json.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("Unterminated string");
        }

        private String readLiteral() {
            int start = pos;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || c == ':' || Character.isWhitespace(c)) {
                    break;
                }
                pos++;
            }
            return json.substring(start, pos);
        }
    }

    // --- Parser ---

    private Object parseValue(Tokenizer t) throws Exception {
        String token = t.nextToken();
        if (token == null) return null;
        switch (token) {
            case "{": return parseObject(t);
            case "[": return parseArray(t);
            case "null": return null;
            default:
                if (token.startsWith("\"")) {
                    // token already has quotes stripped for bare strings; here just return the raw token
                }
                if ("true".equals(token)) return Boolean.TRUE;
                if ("false".equals(token)) return Boolean.FALSE;
                // number
                try {
                    if (token.contains(".")) return Double.parseDouble(token);
                    return Long.parseLong(token);
                } catch (NumberFormatException e) {
                    return token;
                }
        }
    }

    private Object parseString(Tokenizer t) {
        return t.nextToken();
    }

    private Map<String, Object> parseObject(Tokenizer t) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        String token = t.nextToken();
        if ("}".equals(token)) return map;
        // token is key
        String key = token;
        t.nextToken(); // colon
        if (!":".equals(t.nextToken())) throw new RuntimeException("Expected ':'");
        map.put(key, parseValue(t));
        while (true) {
            token = t.nextToken();
            if ("}".equals(token)) break;
            if (!",".equals(token)) throw new RuntimeException("Expected ',' or '}'");
            key = t.nextToken();
            t.nextToken(); // colon
            if (!":".equals(t.nextToken())) throw new RuntimeException("Expected ':'");
            map.put(key, parseValue(t));
        }
        return map;
    }

    private List<Object> parseArray(Tokenizer t) throws Exception {
        List<Object> list = new ArrayList<>();
        String token = t.nextToken();
        if ("]".equals(token)) return list;
        // push back the first value token by using peek/next
        // We already consumed the first token. We need to parse it.
        list.add(convertToken(token, t));
        while (true) {
            token = t.nextToken();
            if ("]".equals(token)) break;
            if (!",".equals(token)) throw new RuntimeException("Expected ',' or ']'");
            list.add(parseValue(t));
        }
        return list;
    }

    private Object convertToken(String token, Tokenizer t) throws Exception {
        if (token == null) return null;
        switch (token) {
            case "{": return parseObject(t);
            case "[": return parseArray(t);
            case "null": return null;
            case "true": return Boolean.TRUE;
            case "false": return Boolean.FALSE;
            default:
                if (token.startsWith("\"") && token.endsWith("\"")) return token.substring(1, token.length() - 1);
                try {
                    if (token.contains(".")) return Double.parseDouble(token);
                    return Long.parseLong(token);
                } catch (NumberFormatException e) {
                    return token;
                }
        }
    }

    // --- Converter ---

    @SuppressWarnings("unchecked")
    private <T> T convert(Object value, Class<T> targetType) throws Exception {
        if (value == null) return null;
        if (targetType.isInstance(value)) return (T) value;

        if (value instanceof Map) {
            // Deserialize to POJO
            Map<String, Object> map = (Map<String, Object>) value;
            T obj = targetType.newInstance();
            for (Field field : obj.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
                Object fieldValue = map.get(field.getName());
                if (fieldValue != null) {
                    field.set(obj, convertField(fieldValue, field));
                }
            }
            return obj;
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (targetType.isArray()) {
                Class<?> componentType = targetType.getComponentType();
                Object arr = Array.newInstance(componentType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(arr, i, convert(list.get(i), componentType));
                }
                return (T) arr;
            }
            if (Set.class.isAssignableFrom(targetType)) {
                Set<Object> set;
                if (LinkedHashSet.class == targetType || HashSet.class == targetType) {
                    set = (Set<Object>) targetType.newInstance();
                } else {
                    set = new LinkedHashSet<>();
                }
                for (Object item : list) {
                    set.add(item);
                }
                return (T) set;
            }
            if (List.class.isAssignableFrom(targetType)) {
                List<Object> resultList = (List<Object>) targetType.newInstance();
                resultList.addAll(list);
                return (T) resultList;
            }
            return null;
        }

        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == int.class || targetType == Integer.class) return (T) Integer.valueOf(num.intValue());
            if (targetType == long.class || targetType == Long.class) return (T) Long.valueOf(num.longValue());
            if (targetType == double.class || targetType == Double.class) return (T) Double.valueOf(num.doubleValue());
            if (targetType == float.class || targetType == Float.class) return (T) Float.valueOf(num.floatValue());
            if (targetType == short.class || targetType == Short.class) return (T) Short.valueOf(num.shortValue());
            if (targetType == byte.class || targetType == Byte.class) return (T) Byte.valueOf(num.byteValue());
        }

        if (value instanceof Boolean && (targetType == boolean.class || targetType == Boolean.class)) {
            return (T) value;
        }

        if (targetType.isEnum() && value instanceof String) {
            return (T) Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
        }

        return (T) value;
    }

    private Object convertField(Object value, Field field) throws Exception {
        Class<?> fieldType = field.getType();
        if (fieldType.isArray()) {
            return convert(value, fieldType);
        }
        if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
                    Class<?> elementType = (Class<?>) typeArgs[0];
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        Object result;
                        if (Set.class.isAssignableFrom(fieldType)) {
                            result = new LinkedHashSet<>();
                        } else {
                            result = new ArrayList<>();
                        }
                        Collection<Object> col = (Collection<Object>) result;
                        for (Object item : list) {
                            col.add(convert(item, elementType));
                        }
                        return col;
                    }
                }
            }
            return convert(value, fieldType);
        }
        return convert(value, fieldType);
    }
}