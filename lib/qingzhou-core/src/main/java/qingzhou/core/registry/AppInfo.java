package qingzhou.core.registry;

import java.io.Serializable;
import java.util.*;

public class AppInfo implements Serializable {
    private String name;
    private String filePath;
    private Properties deploymentProperties;
    private AppState state;
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

    public AppState getState() {
        return state;
    }

    public void setState(AppState state) {
        this.state = state;
    }

    public Map<String, Set<String>> getLoginFreeModelActions() {
        Map<String, Set<String>> openModelActions = new HashMap<>();
        for (ModelInfo modelInfo : modelInfos) {
            for (ModelActionInfo actionInfo : modelInfo.getModelActionInfos()) {
                if (actionInfo.isLoginFree()) {
                    openModelActions.computeIfAbsent(modelInfo.getCode(), s -> new HashSet<>()).add(actionInfo.getCode());
                }
            }
        }
        return openModelActions;
    }

    public Properties getDeploymentProperties() {
        return deploymentProperties;
    }

    public void setDeploymentProperties(Properties deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
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
