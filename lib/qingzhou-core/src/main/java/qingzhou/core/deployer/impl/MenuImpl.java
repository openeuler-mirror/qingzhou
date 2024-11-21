package qingzhou.core.deployer.impl;

import qingzhou.api.Menu;
import qingzhou.core.registry.MenuInfo;

class MenuImpl implements Menu {
    private final MenuInfo newMenuInfo;

    MenuImpl(MenuInfo newMenuInfo) {
        this.newMenuInfo = newMenuInfo;
    }

    @Override
    public Menu icon(String icon) {
        this.newMenuInfo.setIcon(icon);
        return this;
    }

    @Override
    public Menu order(String order) {
        this.newMenuInfo.setOrder(order);
        return this;
    }

    @Override
    public Menu parent(String parent) {
        this.newMenuInfo.setParent(parent);
        return this;
    }

    @Override
    public Menu action(String model, String action) {
        this.newMenuInfo.setModel(model);
        this.newMenuInfo.setAction(action);
        return this;
    }
}
