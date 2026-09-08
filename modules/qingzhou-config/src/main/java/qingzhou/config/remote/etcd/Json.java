package qingzhou.config.remote.etcd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 受限 JSON 读取器：仅支持 etcd v3 JSON Gateway 响应所需的
 * 对象 / 数组 / 字符串 / 数字 / 布尔 / null，解析为 Map/List/String/Number/Boolean。
 */
final class Json {
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("json text is null");
        return new Json(text).readValue();
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= text.length()) throw new IllegalArgumentException("unexpected end of json");
        char c = text.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (text.startsWith(TRUE, pos)) {
            pos += TRUE.length();
            return Boolean.TRUE;
        }
        if (text.startsWith(FALSE, pos)) {
            pos += FALSE.length();
            return Boolean.FALSE;
        }
        if (text.startsWith(NULL, pos)) {
            pos += NULL.length();
            return null;
        }
        return readNumber();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new HashMap<>();
        expect('{');
        skipWhitespace();
        if (consume('}')) return map;
        while (true) {
            skipWhitespace();
            if (pos >= text.length() || text.charAt(pos) != '"') throw new IllegalArgumentException("invalid json object key");
            String key = readString();
            skipWhitespace();
            expect(':');
            map.put(key, readValue());
            skipWhitespace();
            if (consume('}')) return map;
            expect(',');
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (consume(']')) return list;
        while (true) {
            list.add(readValue());
            skipWhitespace();
            if (consume(']')) return list;
            expect(',');
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= text.length()) throw new IllegalArgumentException("invalid json escape");
                char e = text.charAt(pos++);
                switch (e) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("invalid json escape: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("unterminated json string");
    }

    private Object readNumber() {
        int start = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }
        String number = text.substring(start, pos);
        if (number.isEmpty()) throw new IllegalArgumentException("invalid json value");
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException ignored) {
            return Double.parseDouble(number);
        }
    }

    private void expect(char c) {
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw new IllegalArgumentException("expected '" + c + "' at " + pos);
        }
        pos++;
    }

    private boolean consume(char c) {
        if (pos < text.length() && text.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                pos++;
            } else {
                break;
            }
        }
    }
}
