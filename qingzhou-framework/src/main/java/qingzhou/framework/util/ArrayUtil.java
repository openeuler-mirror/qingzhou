package qingzhou.framework.util;

import java.util.Objects;

public class ArrayUtil {
    public static boolean contains(Object[] arr, Object obj) {
        Objects.requireNonNull(arr);
        for (Object o : arr) {
            if (Objects.equals(o, obj)) {
                return true;
            }
        }
        return false;
    }
}
