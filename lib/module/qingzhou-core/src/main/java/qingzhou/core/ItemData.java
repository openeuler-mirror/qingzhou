package qingzhou.core;

import qingzhou.api.Item;

import java.io.Serializable;

public class ItemData implements Serializable {
    private String name;
    private String[] i18n;

    public ItemData(Item item) {
        this.name = item.name();
        this.i18n = item.i18n();
    }

    public ItemData(String name, String[] i18n) {
        this.name = name;
        this.i18n = i18n;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getI18n() {
        return i18n;
    }

    public void setI18n(String[] i18n) {
        this.i18n = i18n;
    }
}
