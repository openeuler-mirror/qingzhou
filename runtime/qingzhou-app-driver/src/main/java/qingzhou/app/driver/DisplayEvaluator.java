package qingzhou.app.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class DisplayEvaluator {

    public static boolean evaluateDisplay(String display, Map<String, String> data) {
        if (display == null) {
            return true;
        }

        String expression = display.trim();
        if (expression.isEmpty()) {
            return true;
        }

        List<List<String>> andGroups = parseDisplay(expression);
        for (List<String> orParts : andGroups) {
            boolean anyTrue = false;
            for (String orPart : orParts) {
                if (evaluateAtomicCondition(orPart, data)) {
                    anyTrue = true;
                    break;
                }
            }
            if (!anyTrue) {
                return false;
            }
        }

        return true;
    }

    private static List<List<String>> parseDisplay(String display) {
        List<List<String>> andGroups = new ArrayList<>();
        String[] andParts = display.split("\\s*&{1,2}\\s*");
        for (String andPart : andParts) {
            String trimmedAndPart = andPart.trim();
            if (trimmedAndPart.isEmpty()) {
                continue;
            }

            String[] orParts = trimmedAndPart.split("\\s*\\|{1,2}\\s*");
            List<String> orGroup = new ArrayList<>();
            for (String orPart : orParts) {
                String trimmedOrPart = orPart.trim();
                if (!trimmedOrPart.isEmpty()) {
                    orGroup.add(trimmedOrPart);
                }
            }
            if (!orGroup.isEmpty()) {
                andGroups.add(orGroup);
            }
        }
        return andGroups;
    }

    private static boolean evaluateAtomicCondition(String condition, Map<String, String> data) {
        AtomicCondition parsed = parseAtomicCondition(condition);
        String rawValue = data.get(parsed.fieldCode);

        switch (parsed.type) {
            case NE:
                return !normalizeValue(rawValue).equals(parsed.expectedValue);
            case EQ:
                return normalizeValue(rawValue).equals(parsed.expectedValue);
            case NOT:
                return !isTruthy(rawValue);
            case TRUTHY:
            default:
                return isTruthy(rawValue);
        }
    }

    private static AtomicCondition parseAtomicCondition(String condition) {
        String trimmed = condition.trim();
        if (trimmed.contains("!=")) {
            int idx = trimmed.indexOf("!=");
            return new AtomicCondition(
                    AtomicCondition.Type.NE,
                    trimmed.substring(0, idx).trim(),
                    trimmed.substring(idx + 2).trim()
            );
        } else if (trimmed.contains("==")) {
            int idx = trimmed.indexOf("==");
            return new AtomicCondition(
                    AtomicCondition.Type.EQ,
                    trimmed.substring(0, idx).trim(),
                    trimmed.substring(idx + 2).trim()
            );
        } else if (trimmed.startsWith("!")) {
            return new AtomicCondition(
                    AtomicCondition.Type.NOT,
                    trimmed.substring(1).trim(),
                    null
            );
        } else {
            return new AtomicCondition(
                    AtomicCondition.Type.TRUTHY,
                    trimmed,
                    null
            );
        }
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Collection<?>) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) > 0;
        }
        return true;
    }

    private static final class AtomicCondition {
        enum Type {
            EQ,
            NE,
            NOT,
            TRUTHY
        }

        final Type type;
        final String fieldCode;
        final String expectedValue;

        AtomicCondition(Type type, String fieldCode, String expectedValue) {
            this.type = type;
            this.fieldCode = fieldCode;
            this.expectedValue = expectedValue;
        }
    }
}