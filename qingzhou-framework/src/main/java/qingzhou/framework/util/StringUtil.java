package qingzhou.framework.util;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    public static String convertStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
    }
}
