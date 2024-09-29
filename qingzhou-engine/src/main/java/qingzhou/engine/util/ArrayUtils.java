package qingzhou.engine.util;

import java.util.Objects;

public class ArrayUtils {


    public static boolean contains(Object[] array, Object obj) {
        if (array == null) {
            return false;
        }
        for (Object o : array) {
            if (Objects.equals(o, obj)) {
                return true;
            }
        }
        return false;
    }
}
