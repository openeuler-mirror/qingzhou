package qingzhou.console;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.data.Datas;
import qingzhou.console.util.ExceptionUtil;
import qingzhou.console.util.ObjectUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasImpl implements Datas, Serializable {
    private transient ConsoleContext context;
    private transient Class<?> dataClassType; // 其结果会转换进入 dataList
    private final List<Map<String, String>> dataList = new ArrayList<>();
    private int totalSize = -1;
    private int pageSize = -1;
    private int pageNum = -1;

    @Override
    public List<Map<String, String>> getDataList() {
        List<Map<String, String>> result = new ArrayList<>(dataList);
        return Collections.unmodifiableList(result);
    }

    @Override
    public void addData(Map<String, String> data) {
        if (data == null) return;
        dataList.add(data);
    }

    @Override
    public void addDataBatch(List<Map<String, String>> list) {
        if (list == null) return;
        dataList.addAll(list);
    }

    @Override
    public void addDataObject(Object data, ConsoleContext context) {
        if (this.dataClassType == null) {
            this.dataClassType = data.getClass();
        }
        if (this.dataClassType != data.getClass()) {
            throw ExceptionUtil.unexpectedException();
        }

        // 直接放入dataList，集中管理远程实例返回值，反序列化时可能找不到实例的model类
        String[] classFields = context.getModelManager().getAllFieldNames(dataClassType);
        Map<String, String> map = new HashMap<>();
        for (String field : classFields) {
            try {
                String objectValue = String.valueOf(ObjectUtil.getObjectValue(data, field));
                map.put(field, objectValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataList.add(Collections.unmodifiableMap(map));

        if (this.context == null) {
            this.context = context;
        } else {
            if (this.context != context) {
                throw ExceptionUtil.unexpectedException();
            }
        }
    }

    public int getTotalSize() {
        return totalSize;
    }

    @Override
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    @Override
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
}
