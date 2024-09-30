package qingzhou.registry;

import java.lang.reflect.Method;

public class ModelActionInfo {
    private transient Method method;
    private String code;
    private String[] name;
    private String[] info;
    private String icon;
    private boolean head;
    private int order;
    private String show;
    private boolean batch;
    private String page;
    private String[] showFields;
    private boolean showToListHead;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public String[] getInfo() {
        return info;
    }

    public void setInfo(String[] info) {
        this.info = info;
    }

    public String getShow() {
        return show;
    }

    public void setShow(String show) {
        this.show = show;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isHead() {
        return head;
    }

    public void setHead(boolean head) {
        this.head = head;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String[] getShowFields() {
        return showFields;
    }

    public void setShowFields(String[] showFields) {
        this.showFields = showFields;
    }

    public boolean isShowToListHead() {
        return showToListHead;
    }

    public void setShowToListHead(boolean showToListHead) {
        this.showToListHead = showToListHead;
    }
}
