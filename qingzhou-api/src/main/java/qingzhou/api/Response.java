package qingzhou.api;

import java.util.Map;

public interface Response {
    void setSuccess(boolean success);

    void setMsg(String msg);

    void setTotalSize(int totalSize);

    void setPageSize(int pageSize);

    void setPageNum(int pageNum);

    void addData(Map<String, String> data);

    void addModelData(ModelBase data) throws Exception;
}
