package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

public interface List extends QingzhouModel {
    String ACTION_CODE_LIST = "list";

    java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception;

    default int totalSize(Map<String, String> query) {
        return -1; // -1：不使用分页
    }

    boolean contains(String id);
}
