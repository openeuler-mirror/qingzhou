package qingzhou.console.page;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private String menuName = "";
    private String menuIcon = "";
    private String menuAction = "#";
    private String[] i18ns = new String[0];
    private final List<MenuItem> children = new ArrayList<>();
    private int order = 0;

    String getMenuName() {
        return menuName;
    }

    void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    String getMenuIcon() {
        return menuIcon;
    }

    void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    String getMenuAction() {
        return menuAction;
    }

    void setMenuAction(String menuAction) {
        this.menuAction = menuAction;
    }

    String[] getI18ns() {
        return i18ns;
    }

    void setI18ns(String[] i18ns) {
        this.i18ns = i18ns;
    }

    List<MenuItem> getChildren() {
        return children;
    }

    int getOrder() {
        return order;
    }

    void setOrder(int order) {
        this.order = order;
    }
}
