package qingzhou.api.console.data;

import java.util.List;
import java.util.Map;

public interface Response {
    void setSuccess(boolean success);

    boolean isSuccess();

    void setMsg(String msg);

    String getMsg();

    void setContentType(String contentType);

    String getContentType();

    void addData(Map<String, String> data);

    void addDataObject(Object data) throws Exception;

    List<Map<String, String>> getDataList();

    void setTotalSize(int totalSize);

    int getTotalSize();

    void setPageSize(int pageSize);

    int getPageSize();

    void setPageNum(int pageNum);

    int getPageNum();
}
