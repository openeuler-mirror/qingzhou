package qingzhou.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MenuInfo {
    private String name;
    private String[] i18n;
    private String icon;
    private int order;
    private List<MenuInfo> children; // 用于存储子菜单

    public MenuInfo(String name, String[] i18n, String icon, int order) {
        this.name = name;
        this.i18n = i18n;
        this.icon = icon;
        this.order = order;
        this.children = new ArrayList<>(); // 初始化子菜单列表
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<MenuInfo> getChildren() {
        return children; // 添加 getter 方法
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuInfo menuInfo = (MenuInfo) o;
        return Objects.equals(name, menuInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
