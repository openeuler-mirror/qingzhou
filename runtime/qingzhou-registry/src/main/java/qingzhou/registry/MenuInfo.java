package qingzhou.registry;

import java.util.Objects;

public class MenuInfo {
    private String menuName;
    private String[] menuI18n;
    private String menuIcon;
    private int menuOrder;

    public MenuInfo() {
    }

    public MenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        this.menuName = menuName;
        this.menuI18n = menuI18n;
        this.menuIcon = menuIcon;
        this.menuOrder = menuOrder;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String[] getMenuI18n() {
        return menuI18n;
    }

    public void setMenuI18n(String[] menuI18n) {
        this.menuI18n = menuI18n;
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    public int getMenuOrder() {
        return menuOrder;
    }

    public void setMenuOrder(int menuOrder) {
        this.menuOrder = menuOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuInfo menuInfo = (MenuInfo) o;
        return Objects.equals(menuName, menuInfo.menuName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuName);
    }
}
