package qingzhou.deployer.impl;

import qingzhou.api.metadata.ModelData;

import java.io.Serializable;

public class ModelDataImpl implements ModelData, Serializable {
    private String name;
    private String icon;
    private String[] nameI18n;
    private String[] infoI18n;
    private String entryAction;
    private boolean showToMenu = true;
    private String menuName = "";
    private int menuOrder = 0;

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String icon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String[] nameI18n() {
        return nameI18n;
    }

    public void setNameI18n(String[] nameI18n) {
        this.nameI18n = nameI18n;
    }

    public String[] infoI18n() {
        return infoI18n;
    }

    public void setInfoI18n(String[] infoI18n) {
        this.infoI18n = infoI18n;
    }

    public String entryAction() {
        return entryAction;
    }

    public void setEntryAction(String entryAction) {
        this.entryAction = entryAction;
    }

    public boolean showToMenu() {
        return showToMenu;
    }

    public void setShowToMenu(boolean showToMenu) {
        this.showToMenu = showToMenu;
    }

    public String menuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public int menuOrder() {
        return menuOrder;
    }

    public void setMenuOrder(int menuOrder) {
        this.menuOrder = menuOrder;
    }

}
