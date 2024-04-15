package qingzhou.engine.util;

import java.text.DecimalFormat;

public class StringUtil {

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean notBlank(String value) {
        return !isBlank(value);
    }

    public static String convertStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
    }

    public static String convertGBytes(long val) {
        double v = ((double) val) / 1024 / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    public static String convertMBytes(long val) {
        double v = ((double) val) / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }
}
