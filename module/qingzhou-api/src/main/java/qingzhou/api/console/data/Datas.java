package qingzhou.api.console.data;

import qingzhou.api.console.ConsoleContext;

import java.util.List;
import java.util.Map;

public interface Datas {// todo 此对象里面的分页参数不应该暴漏给用户？

    void addData(Map<String, String> data);

    void addDataBatch(List<Map<String, String>> list);

    void addDataObject(Object data, ConsoleContext context);// todo ConsoleContext 在这里是否合适？

    List<Map<String, String>> getDataList();

    int getTotalSize();

    void setTotalSize(int totalSize);

    int getPageSize();

    void setPageSize(int pageSize);

    int getPageNum();

    void setPageNum(int pageNum);
}
