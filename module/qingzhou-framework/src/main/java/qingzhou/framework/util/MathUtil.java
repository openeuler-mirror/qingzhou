package qingzhou.framework.util;

import java.text.DecimalFormat;

public class MathUtil {
    public static String maskMBytes(long val) {
        double v = ((double) val) / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    private MathUtil() {
    }
}
