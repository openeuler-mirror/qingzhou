package qingzhou.api.action;

import java.util.List;
import java.util.Map;

import qingzhou.api.QingzhouModel;

public interface Page extends QingzhouModel {
    String ACTION_CODE_PAGE = "page";

    List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception;

    default int totalSize(Map<String, String> query) {
        return -1; // -1：不使用分页
    }

    boolean contains(String id);
}
