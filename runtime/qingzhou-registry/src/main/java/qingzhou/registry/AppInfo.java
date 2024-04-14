package qingzhou.registry;

import java.util.HashSet;
import java.util.Set;

public class AppInfo {
    private String name;
    private ModelInfo[] modelInfos;
    private Set<MenuInfo> menuInfos;

    public synchronized Set<MenuInfo> getMenuInfos() {
        if (menuInfos == null) {
            menuInfos = new HashSet<>();
        }
        return menuInfos;
    }

    public void setMenuInfos(Set<MenuInfo> menuInfos) {
        this.menuInfos = menuInfos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModelInfo[] getModelInfos() {
        return modelInfos;
    }

    public void setModelInfos(ModelInfo[] modelInfos) {
        this.modelInfos = modelInfos;
    }
}
