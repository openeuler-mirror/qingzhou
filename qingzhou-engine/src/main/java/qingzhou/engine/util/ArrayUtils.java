package qingzhou.engine.util;

import java.util.ArrayList;
import java.util.List;
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

    public static Object[] overlap(Object[] arr1, Object[] arr2) {
        if (arr1 == null || arr2 == null) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (Object o1 : arr1) {
            for (Object o2 : arr2) {
                if (Objects.equals(o1, o2)) {
                    list.add(o1);
                }
            }
        }
        return list.toArray();
    }

}
