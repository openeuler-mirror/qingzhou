package qingzhou.app;

import qingzhou.framework.app.Menu;

import java.io.Serializable;
import java.util.Objects;

public class MenuImpl implements Menu, Serializable {
    private final String menuName;
    private String[] menuI18n;
    private String menuIcon;
    private int menuOrder;

    public MenuImpl(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public String getName() {
        return menuName;
    }

    @Override
    public String[] getI18n() {
        return menuI18n;
    }

    public void setMenuI18n(String[] menuI18n) {
        this.menuI18n = menuI18n;
    }

    @Override
    public String getIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    @Override
    public int getOrder() {
        return menuOrder;
    }

    public void setMenuOrder(int menuOrder) {
        this.menuOrder = menuOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuImpl menu = (MenuImpl) o;
        return Objects.equals(menuName, menu.menuName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuName);
    }
}
