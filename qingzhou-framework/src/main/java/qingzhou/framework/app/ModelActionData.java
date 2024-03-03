package qingzhou.framework.app;

import java.io.Serializable;

public class ModelActionData implements Serializable {
    private String name;
    private String icon = "";
    private String[] nameI18n;
    private String[] infoI18n;
    private String effectiveWhen = "";
    private String forwardToPage = "";
    private boolean showToListHead = false;
    private boolean showToList = false;
    private int orderOnList = 0;
    private boolean supportBatch = false;
    private boolean disabled = false;

    public String name() {
        return name;
    }

    public String icon() {
        return icon;
    }

    public String[] nameI18n() {
        return nameI18n.clone();
    }

    public String[] infoI18n() {
        return infoI18n.clone();
    }

    public String effectiveWhen() {
        return effectiveWhen;
    }

    public String forwardToPage() {
        return forwardToPage;
    }

    public boolean showToListHead() {
        return showToListHead;
    }

    public boolean showToList() {
        return showToList;
    }

    public int orderOnList() {
        return orderOnList;
    }

    public boolean supportBatch() {
        return supportBatch;
    }

    public boolean disabled() {
        return disabled;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setEffectiveWhen(String effectiveWhen) {
        this.effectiveWhen = effectiveWhen;
    }

    public void setForwardToPage(String forwardToPage) {
        this.forwardToPage = forwardToPage;
    }

    public void setShowToListHead(boolean showToListHead) {
        this.showToListHead = showToListHead;
    }

    public void setShowToList(boolean showToList) {
        this.showToList = showToList;
    }

    public void setOrderOnList(int orderOnList) {
        this.orderOnList = orderOnList;
    }

    public void setSupportBatch(boolean supportBatch) {
        this.supportBatch = supportBatch;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameI18n(String[] nameI18n) {
        this.nameI18n = nameI18n;
    }

    public void setInfoI18n(String[] infoI18n) {
        this.infoI18n = infoI18n;
    }
}
