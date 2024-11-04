package qingzhou.deployer;

import qingzhou.api.MsgLevel;
import qingzhou.api.Response;
import qingzhou.registry.ItemInfo;

import java.io.Serializable;
import java.util.*;

public class ResponseImpl implements Response {
    private boolean success = true;
    private String msg;
    private MsgLevel msgLevel;
    private String contentType;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> parametersInSession = new HashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Long> dateHeaders = new LinkedHashMap<>();
    private byte[] bodyBytes;
    private String downloadName;
    private ItemInfo[] itemInfos;
    private String[] ids;

    private Serializable customizedDataObject;

    private final Map<String, String> dataMap = new LinkedHashMap<>();

    private final Map<String, String> errorInfo = new LinkedHashMap<>();

    private List<String[]> dataList = new ArrayList<>();
    private int totalSize = -1;
    private int pageSize = -1;
    private int pageNum = -1;

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

    @Override
    public void setMsgLevel(MsgLevel msgLevel) {
        this.msgLevel = msgLevel;
    }

    public MsgLevel getMsgLevel() {
        return this.msgLevel;
    }

    public void addErrorInfo(String key, String value) {
        errorInfo.put(key, value);
    }

    public Map<String, String> getErrorInfo() {
        return errorInfo;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void setDateHeader(String name, long date) {
        dateHeaders.put(name, date);
    }

    public Long getDateHeader(String name) {
        return dateHeaders.get(name);
    }

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

    public void setDownloadName(String downloadName) {
        this.downloadName = downloadName;
    }

    public Serializable getCustomizedDataObject() {
        return customizedDataObject;
    }

    @Override
    public void useCustomizedResponse(Serializable customizedDataObject) {
        this.customizedDataObject = customizedDataObject;
    }

    public Map<String, String> getDataMap() {
        return dataMap;
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

    public void setDataList(List<String[]> dataList) {
        this.dataList = dataList;
    }

    public List<String[]> getDataList() {
        return dataList;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }
}
