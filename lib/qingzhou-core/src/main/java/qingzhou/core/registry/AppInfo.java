package qingzhou.core.registry;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class AppInfo implements Serializable {
    private String name;
    private String filePath;
    private String state;
    private final Set<ModelInfo> modelInfos = new LinkedHashSet<>();
    private final Set<MenuInfo> menuInfos = new LinkedHashSet<>();

    public void removeModelInfo(ModelInfo modelInfo) {
        modelInfos.remove(modelInfo);
    }

    public void addModelInfo(ModelInfo modelInfo) {
        boolean added = modelInfos.add(modelInfo);
        if (!added) {
            throw new IllegalArgumentException("The same model already exists: " + modelInfo.getCode());
        }
    }

    public ModelInfo getModelInfo(String modelName) {
        return modelInfos.stream().filter(modelInfo -> modelInfo.getCode().equals(modelName)).findAny().orElse(null);
    }

    public void addMenuInfo(MenuInfo menuInfo) {
        boolean added = menuInfos.add(menuInfo);
        if (!added) {
            throw new IllegalArgumentException("The same menu already exists: " + menuInfo.getName());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    // 返回有序的模块列表
    public ModelInfo[] getModelInfos() {
        return modelInfos.toArray(new ModelInfo[0]);
    }

    // 返回有序的菜单列表
    public MenuInfo[] getMenuInfos() {
        return menuInfos.toArray(new MenuInfo[0]);
    }
}
