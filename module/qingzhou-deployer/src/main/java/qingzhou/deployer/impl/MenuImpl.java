package qingzhou.deployer.impl;

import qingzhou.api.Menu;
import qingzhou.registry.MenuInfo;

class MenuImpl implements Menu {
    private final MenuInfo newMenuInfo;

    MenuImpl(MenuInfo newMenuInfo) {
        this.newMenuInfo = newMenuInfo;
    }

    @Override
    public Menu setIcon(String icon) {
        this.newMenuInfo.setIcon(icon);
        return this;
    }

    @Override
    public Menu setOrder(int order) {
        this.newMenuInfo.setOrder(order);
        return this;
    }

    @Override
    public Menu setParent(String parent) {
        this.newMenuInfo.setParent(parent);
        return this;
    }
}
