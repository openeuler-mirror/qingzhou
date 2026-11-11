package qingzhou.api.action;

import java.util.List;
import java.util.Map;

public interface Page {
    List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception;

    default int total(Map<String, String> query) {
        return -1; // -1：不使用分页
    }

    boolean contains(String id);
}
