package qingzhou.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryDataStoreDemo {
    private final String idKey;
    private final List<Map<String, String>> dataList = new ArrayList<>();

    public MemoryDataStoreDemo(String idKey) {
        this.idKey = idKey;
    }

    public void addData(Map<String, String> data) {
        dataList.add(data);
    }

    public void deleteData(String id) {
        dataList.removeIf(data -> data.get(idKey).equals(id));
    }

    public Map<String, String> showData(String id) {
        for (Map<String, String> data : dataList) {
            if (data.get(idKey).equals(id)) {
                return data;
            }
        }
        return null;
    }

    public void updateData(String id, Map<String, String> data) {
        showData(id).putAll(data);
    }

    public String[] allIds(Map<String, String> query) {
        return dataList.stream().filter(data -> query(query, data))
                .map(data -> data.get(idKey))
                .toArray(String[]::new);
    }

    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames, Map<String, String> query) {
        String[] ids = allIds(query);
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, ids.length);

        List<Map<String, String>> result = new ArrayList<>();
        for (String id : Arrays.copyOfRange(ids, fromIndex, endIndex)) {
            Map<String, String> item = showData(id);
            result.add(new HashMap<String, String>() {{
                for (String fieldName : fieldNames) {
                    put(fieldName, item.get(fieldName));
                }
            }});
        }
        return result;
    }

    public int totalSize(Map<String, String> query) {
        return allIds(query).length;
    }

    // 实现参考：qingzhou.app.system.ModelUtil.query
    public static boolean query(Map<String, String> query, Map<String, String> data) {
        if (query == null) return true;

        // 实现参考：qingzhou.app.system.ModelUtil.query
        for (Map.Entry<String, String> e : query.entrySet()) {
            String queryKey = e.getKey();
            String queryValue = e.getValue();

            String val = data.get(queryKey);

            String querySP = ",";
            if (queryValue.contains(querySP)) {
                boolean found = false;
                for (String q : queryValue.split(querySP)) {
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
}
