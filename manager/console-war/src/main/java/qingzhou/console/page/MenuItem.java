package qingzhou.console.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import qingzhou.registry.ModelInfo;

class MenuItem {
    private String name = "";
    private String icon = "";
    private String[] i18n = new String[0];
    private final List<MenuItem> children = new ArrayList<>();
    private final List<ModelInfo> modelInfos = new ArrayList<>();

    void addModelInfo(ModelInfo modelInfo) {
        modelInfos.add(modelInfo);
        modelInfos.sort(Comparator.comparingInt(ModelInfo::getOrder));
    }

    public List<ModelInfo> getModelInfos() {
        return modelInfos;
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

    List<MenuItem> getChildren() {
        return children;
    }
}
