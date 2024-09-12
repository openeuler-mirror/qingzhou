package qingzhou.app;

import java.util.*;

public class MemoryDataStoreDemo {
    private final String idKey;
    private final List<Map<String, String>> dataList = new ArrayList<>();

    public MemoryDataStoreDemo(String idKey) {
        this.idKey = idKey;
    }

    public void addData(String id, Map<String, String> data) {
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

    public String[] allIds() {
        return dataList.stream().map(data -> data.get(idKey)).toArray(String[]::new);
    }

    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(pageNum * pageSize - 1, dataList.size());

        List<Map<String, String>> result = new ArrayList<>();
        for (String id : Arrays.copyOfRange(allIds(), fromIndex, toIndex)) {
            Map<String, String> item = showData(id);
            result.add(new HashMap<String, String>() {{
                for (String fieldName : fieldNames) {
                    put(fieldName, item.get(fieldName));
                }
            }});

        }
        return result;
    }

    public int totalSize() {
        return dataList.size();
    }
}
