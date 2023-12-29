package qingzhou.console;

import qingzhou.api.console.data.Response;
import qingzhou.console.util.DownLoadUtil;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseImpl implements Response, Serializable {
    private boolean success = true;
    private String msg;
    private String contentType;
    private final List<Map<String, String>> dataList = new ArrayList<>();
    private int totalSize = -1;
    private int pageSize = -1;
    private int pageNum = -1;

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
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

    @Override
    public List<Map<String, String>> getDataList() {
        return dataList;
    }

    @Override
    public void addData(Map<String, String> data) {
        if (data == null) return;
        dataList.add(data);
    }

    @Override
    public void addDataObject(Object data) throws Exception {
        Map<String, String> map = new HashMap<>();

        BeanInfo beanInfo = Introspector.getBeanInfo(data.getClass());
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            Method readMethod = pd.getReadMethod();
            if (readMethod != null) {
                Object val = readMethod.invoke(data);
                map.put(pd.getName(), String.valueOf(val));
            }
        }

        dataList.add(map);
    }

    public Map<String, Object> downloadData() {
        return null;// todo
    }

    public void readDownloadFile(String key, long offset, File[] downloadFiles) throws Exception {
        if (key == null) {
            if (downloadFiles == null || downloadFiles.length == 0) {
                return;
            }
            key = DownLoadUtil.buildDownloadKey(downloadFiles);
        }
        // downloadData = DownLoadUtil.downloadFile(key, offset); todo
    }
}
