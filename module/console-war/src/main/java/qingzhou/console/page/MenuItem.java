package qingzhou.console.page;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {

    private String parentMenu = "#";
    private String menuName = "";
    private String menuIcon = "";
    private String menuAction = "#";
    private String[] i18ns = new String[0];
    private List<MenuItem> children = new ArrayList<>();
    private int order = 0;

    public String getParentMenu() {
        return parentMenu;
    }

    public void setParentMenu(String parentMenu) {
        this.parentMenu = parentMenu;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    public String getMenuAction() {
        return menuAction;
    }

    public void setMenuAction(String menuAction) {
        this.menuAction = menuAction;
    }

    public String[] getI18ns() {
        return i18ns;
    }

    public void setI18ns(String[] i18ns) {
        this.i18ns = i18ns;
    }

    public List<MenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItem> children) {
        this.children = children;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
