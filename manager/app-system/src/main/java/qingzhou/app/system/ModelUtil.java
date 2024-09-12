package qingzhou.app.system;

import java.util.*;

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

    public static List<Map<String, String>> listData(String[] allIds, Supplier supplier,
                                                     int pageNum, int pageSize, String[] fieldNames) {
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        for (String id : subList) {
            Map<String, String> result = new HashMap<>();

            Map<String, String> idData = supplier.get(id);
            for (String fieldName : fieldNames) {
                result.put(fieldName, idData.get(fieldName));
            }

            data.add(result);
        }
        return data;
    }

    public interface Supplier {
        Map<String, String> get(String id);
    }
}
