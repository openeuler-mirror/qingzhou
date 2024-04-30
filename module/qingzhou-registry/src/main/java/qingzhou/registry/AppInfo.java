package qingzhou.registry;

import java.util.Collection;

public class AppInfo {
    private String name;
    private Collection<ModelInfo> modelInfos;
    private Collection<MenuInfo> menuInfos;

    public MenuInfo getMenuInfo(String name) {
        return menuInfos.stream().filter(menuInfo -> menuInfo.getName().equals(name)).findAny().orElse(null);
    }

    public ModelInfo getModelInfo(String modelName) {
        for (ModelInfo modelInfo : modelInfos) {
            if (modelInfo.getCode().equals(modelName)) {
                return modelInfo;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<ModelInfo> getModelInfos() {
        return modelInfos;
    }

    public void setModelInfos(Collection<ModelInfo> modelInfos) {
        this.modelInfos = modelInfos;
    }

    public Collection<MenuInfo> getMenuInfos() {
        return menuInfos;
    }

    public void setMenuInfos(Collection<MenuInfo> menuInfos) {
        this.menuInfos = menuInfos;
    }
}
