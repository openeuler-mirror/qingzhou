package qingzhou.registry;

import java.util.Objects;

public class MenuInfo {
    private String name;
    private String[] i18n;
    private String icon;
    private int order;

    public MenuInfo(String name, String[] i18n, String icon, int order) {
        this.name = name;
        this.i18n = i18n;
        this.icon = icon;
        this.order = order;
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
