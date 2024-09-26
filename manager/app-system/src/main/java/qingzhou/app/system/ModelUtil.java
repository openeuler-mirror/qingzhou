package qingzhou.app.system;

import java.util.*;

public class ModelUtil {
    public static boolean query(Map<String, String> query, Supplier supplier) {
        if (query == null) return true;

        for (Map.Entry<String, String> e : query.entrySet()) {
            String queryKey = e.getKey();
            String queryValue = e.getValue();
            Map<String, String> data = supplier.get();
            String val = data.get(queryKey);
            if (val == null) return false;
            if (queryValue.contains(",")) {
                boolean found = false;
                for (String q : queryValue.split("\\|")) {
                    if (val.equals(q)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            } else {
                if (!val.contains(queryValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Map<String, String>> listData(String[] allIds, IdSupplier idSupplier,
                                                     int pageNum, int pageSize, String[] fieldNames) {
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        List<Map<String, String>> data = new ArrayList<>();
        for (String id : subList) {
            Map<String, String> result = new HashMap<>();

            Map<String, String> idData = idSupplier.get(id);
            for (String fieldName : fieldNames) {
                result.put(fieldName, idData.get(fieldName));
            }

            data.add(result);
        }
        return data;
    }

    public interface IdSupplier {
        Map<String, String> get(String id);
    }

    public interface Supplier {
        Map<String, String> get();
    }
}
