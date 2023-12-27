package qingzhou.api.console.data;

import qingzhou.api.console.ConsoleContext;

import java.util.List;
import java.util.Map;

public interface Datas {
    void addData(Map<String, String> data);

    void addDataBatch(List<Map<String, String>> list);

    void addDataObject(Object data, ConsoleContext context);// 对象会被序列化为Map在进入addData

    List<Map<String, String>> getDataList();

    int getTotalSize();

    void setTotalSize(int totalSize);

    int getPageSize();

    void setPageSize(int pageSize);

    int getPageNum();

    void setPageNum(int pageNum);
}
