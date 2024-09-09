package qingzhou.app;

import java.util.*;

public class MemoryDataStore implements DataStore {
    private final LinkedHashMap<String, Map<String, String>> dataList = new LinkedHashMap<>();

    @Override
    public void addData(String id, Map<String, String> data) {
        dataList.put(id, data);
    }

    @Override
    public void deleteData(String id) {
        dataList.remove(id);
    }

    @Override
    public Map<String, String> showData(String id) {
        return dataList.get(id);
    }

    @Override
    public void updateData(String id, Map<String, String> data) {
        dataList.get(id).putAll(data);
    }

    @Override
    public List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) {
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(pageNum * pageSize - 1, dataList.size());

        List<Map<String, String>> result = new ArrayList<>();
        new ArrayList<>(dataList.keySet()).subList(fromIndex, toIndex).forEach(s -> {
            Map<String, String> item = dataList.get(s);
            result.add(new HashMap<String, String>() {{
                for (String fieldName : fieldNames) {
                    put(fieldName, item.get(fieldName));
                }
            }});

        });
        return result;
    }

    @Override
    public int totalSize() {
        return dataList.size();
    }
}
