package qingzhou.app.impl;

import qingzhou.api.Group;

import java.io.Serializable;

public class GroupImpl implements Group, Serializable {
    public final String name;
    public final String[] i18n;

    public GroupImpl(String name, String[] i18n) {
        this.name = name;
        this.i18n = i18n;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String[] i18n() {
        return i18n;
    }
}
