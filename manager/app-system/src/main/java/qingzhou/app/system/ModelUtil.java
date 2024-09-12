package qingzhou.app.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelUtil {
    public static List<Map<String, String>> listData(List<Map<String, String>> allData,
                                                     int pageNum, int pageSize, String[] fieldNames) {
        int totalSize = allData.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        List<Map<String, String>> subList = allData.subList(startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        subList.forEach(originData -> data.add(new HashMap<String, String>() {{
            for (String fieldName : fieldNames) {
                put(fieldName, originData.get(fieldName));
            }
        }}));
        return data;
    }

    public static List<Map<String, String>> listData(List<String> allIds, Supplier supplier,
                                                     int pageNum, int pageSize, String[] fieldNames) {
        int totalSize = allIds.size();
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        List<String> subList = allIds.subList(startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        subList.forEach(a -> data.add(new HashMap<String, String>() {{
            Map<String, String> data = supplier.get(a);
            for (String fieldName : fieldNames) {
                put(fieldName, data.get(fieldName));
            }
        }}));
        return data;
    }

    public interface Supplier {
        Map<String, String> get(String id);
    }
}
