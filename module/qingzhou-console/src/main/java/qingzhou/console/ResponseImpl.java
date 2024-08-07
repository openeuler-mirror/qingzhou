package qingzhou.console;

import qingzhou.api.ModelBase;
import qingzhou.api.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseImpl implements Response {
    private boolean success = true;
    private String msg;
    private final List<Map<String, String>> dataList = new ArrayList<>();
    private int totalSize = -1;
    private int pageSize = -1;
    private int pageNum = -1;
    private String contentType;
    private final Map<String, String> parametersInSession = new HashMap<>();

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }

    public int getTotalSize() {
        return totalSize;
    }

    @Override
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

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

    public List<Map<String, String>> getDataList() {
        return dataList;
    }

    @Override
    public void addData(Map<String, String> data) {
        if (data == null) return;
        dataList.add(data);
    }

    @Override
    public void addModelData(ModelBase data) {
        Map<String, String> map = new HashMap<>();

        Field[] fields = data.getClass().getFields();
        for (Field field : fields) {
            try {
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                    map.put(field.getName(), String.valueOf(field.get(data)));
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        dataList.add(map);
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getParametersInSession() {
        return parametersInSession;
    }
}
