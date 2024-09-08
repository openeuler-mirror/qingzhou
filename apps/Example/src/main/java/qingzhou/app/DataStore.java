package qingzhou.app;

import java.util.List;
import java.util.Map;

public interface DataStore {
    void addData(String id, Map<String, String> data);

    void deleteData(String id);

    Map<String, String> showData(String id);

    void updateData(String id, Map<String, String> data);

    List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames);

    int totalSize();
}
