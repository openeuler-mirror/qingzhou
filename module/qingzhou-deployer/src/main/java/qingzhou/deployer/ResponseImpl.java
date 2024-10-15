package qingzhou.deployer;

import qingzhou.api.ModelBase;
import qingzhou.api.MsgLevel;
import qingzhou.api.Response;
import qingzhou.registry.ItemInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ResponseImpl implements Response {
    private boolean success = true;
    private String msg;
    private MsgLevel msgLevel;
    private final List<Map<String, String>> dataList = new ArrayList<>();
    private int totalSize = -1;
    private int pageSize = -1;
    private int pageNum = -1;
    private String contentType;
    private final Map<String, String> parametersInSession = new HashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Long> dateHeaders = new LinkedHashMap<>();
    private byte[] bodyBytes;
    private String downloadName;
    private final Map<String, String> parameters = new HashMap<>();
    private ItemInfo[] itemInfos;

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
    public void setMsgType(MsgLevel msgLevel) {
        this.msgLevel = msgLevel;
    }

    @Override
    public MsgLevel getMsgType() {
        return this.msgLevel;
    }

    @Override
    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public int getPageNum() {
        return pageNum;
    }

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
    public void addModelData(ModelBase data) {
        Map<String, String> map = new HashMap<>();

        Field[] fields = data.getClass().getFields();
        for (Field field : fields) {
            try {
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                    Object val = field.get(data);
                    if (val != null) {
                        map.put(field.getName(), String.valueOf(val));
                    }
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

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void setDateHeader(String name, long date) {
        dateHeaders.put(name, date);
    }

    @Override
    public Long getDateHeader(String name) {
        return dateHeaders.get(name);
    }

    @Override
    public Collection<String> getDateHeaderNames() {
        return dateHeaders.keySet();
    }

    public void setBodyBytes(byte[] bytes) {
        bodyBytes = bytes;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public String getDownloadName() {
        return downloadName;
    }

    @Override
    public void setDownloadName(String downloadName) {
        this.downloadName = downloadName;
    }

    public Map<String, String> getParametersInSession() {
        return parametersInSession;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public ItemInfo[] getItemInfos() {
        return itemInfos;
    }

    public void setItemInfos(ItemInfo[] itemInfos) {
        this.itemInfos = itemInfos;
    }
}
