package qingzhou.console.page;

import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class MenuItem {
    private String name;
    private String icon;
    private String order;
    private String[] i18n = new String[0];
    private final List<MenuItem> subMenuList = new ArrayList<>();
    private final List<ModelInfo> subModelList = new ArrayList<>();
    private String model;
    private String action;

    MenuItem findMenu(String name) {
        if (Utils.isBlank(name)) return null;
        if (name.equals(this.getName())) return this;
        for (MenuItem subMenu : subMenuList) {
            MenuItem found = subMenu.findMenu(name);
            if (found != null) return found;
        }
        return null;
    }

    void addModelInfo(ModelInfo modelInfo) {
        subModelList.add(modelInfo);
        subModelList.sort(Comparator.comparing(ModelInfo::getOrder));
    }

    List<ModelInfo> getSubModelList() {
        return subModelList;
    }

    void addMenuItem(MenuItem menuItem) {
        subMenuList.add(menuItem);
        subMenuList.sort(Comparator.comparing(MenuItem::getOrder));
    }

    List<MenuItem> getSubMenuList() {
        return subMenuList;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getIcon() {
        return icon;
    }

    void setIcon(String icon) {
        this.icon = icon;
    }

    String[] getI18n() {
        return i18n;
    }

    void setI18n(String[] i18n) {
        this.i18n = i18n;
    }

    String getOrder() {
        return order;
    }

    void setOrder(String order) {
        this.order = order;
    }

    String getModel() {
        return model;
    }

    void setModel(String model) {
        this.model = model;
    }

    String getAction() {
        return action;
    }

    void setAction(String action) {
        this.action = action;
    }
}
