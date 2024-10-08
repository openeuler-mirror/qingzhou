package qingzhou.registry;

import java.util.LinkedHashSet;
import java.util.Set;

public class AppInfo {
    private String name;
    private String filePath;
    private final Set<ModelInfo> modelInfos = new LinkedHashSet<>();
    private final Set<MenuInfo> menuInfos = new LinkedHashSet<>();

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

    public Set<ModelInfo> getModelInfos() {
        return modelInfos;
    }

    public Set<MenuInfo> getMenuInfos() {
        return menuInfos;
    }
}
