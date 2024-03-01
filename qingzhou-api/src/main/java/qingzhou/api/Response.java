package qingzhou.api;

import java.util.List;
import java.util.Map;

public interface Response {
    void setSuccess(boolean success);

    void setMsg(String msg);

    void setTotalSize(int totalSize);

    void setPageSize(int pageSize);

    void setPageNum(int pageNum);

    void addData(Map<String, String> data);

    void addDataObject(Object data) throws Exception;

    boolean isSuccess();

    List<Map<String, String>> getDataList();

    int getTotalSize();

    int getPageSize();
}
