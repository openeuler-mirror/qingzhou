package qingzhou.app;

import qingzhou.api.ModelBase;
import qingzhou.api.type.General;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MemoryDataStoreModelBase extends ModelBase implements General {

    private final String idKey;
    private final List<Map<String, String>> dataList = new ArrayList<>();

    public MemoryDataStoreModelBase(String idField) {
        idKey = idField;
    }

    @Override
    public void addData(Map<String, String> data) {
        dataList.add(data);
        ExampleMain.logger.info("addData:" + data);
    }

    @Override
    public void deleteData(String id) {
        dataList.removeIf(data -> data.get(idKey).equals(id));
        ExampleMain.logger.info("deleteData:" + id);
    }

    private String[] allIds(Map<String, String> query) {
        return dataList.stream().filter(data -> query(query, data))
                .map(data -> data.get(idKey))
                .toArray(String[]::new);
    }

    // 实现参考：qingzhou.app.master.ModelUtil.query
    public static boolean query(Map<String, String> query, Map<String, String> data) {
        if (query == null)
            return true;

        // 实现参考：qingzhou.app.master.ModelUtil.query
        for (Map.Entry<String, String> e : query.entrySet()) {
            String queryKey = e.getKey();
            String queryValue = e.getValue();

            String val = data.get(queryKey);
            if (val == null)
                return false;

            String querySP = ",";
            if (queryValue.contains(querySP)) {
                boolean found = false;
                for (String q : queryValue.split(querySP)) {
                    if (val.equals(q)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return false;
            } else {
                if (!val.contains(queryValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) {
        String[] ids = allIds(query);
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, ids.length);

        List<String[]> result = new ArrayList<>();
        for (String id : Arrays.copyOfRange(ids, fromIndex, endIndex)) {
            Map<String, String> item = showData(id);
            String[] data = new String[showFields.length];
            for (int i = 0; i < showFields.length; i++) {
                data[i] = item.get(showFields[i]);
            }
            result.add(data);
        }
        return result;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return allIds(query).length;
    }

    @Override
    public Map<String, String> showData(String id) {
        for (Map<String, String> data : dataList) {
            if (data.get(idKey).equals(id)) {
                return data;
            }
        }
        return null;
    }

    @Override
    public void updateData(Map<String, String> data) {
        showData(getAppContext().getCurrentRequest().getId()).putAll(data);
        ExampleMain.logger.info("updateData:" + data);
    }

    @Override
    public int pageSize() {
        return 3;
    }

    @Override
    public boolean contains(String id) {
        return showData(id) != null;
    }
}
