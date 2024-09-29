package qingzhou.registry;

import java.util.Arrays;
import java.util.Comparator;

public class AppInfo {
    private String name;
    private String filePath;
    private ModelInfo[] modelInfos = new ModelInfo[0];
    private MenuInfo[] menuInfos = new MenuInfo[0];

    public MenuInfo getMenuInfo(String name) {
        if (menuInfos != null) {
            for (MenuInfo menuInfo : menuInfos) {
                if (menuInfo.getName().equals(name)) {
                    return menuInfo;
                }
            }
        }
        return null;
    }

    public ModelInfo getModelInfo(String modelName) {
        for (ModelInfo modelInfo : modelInfos) {
            if (modelInfo.getCode().equals(modelName)) {
                return modelInfo;
            }
        }
        return null;
    }

    public void addMenuInfo(MenuInfo menuInfo) {
        MenuInfo[] newMenuInfos = new MenuInfo[menuInfos.length + 1];
        System.arraycopy(menuInfos, 0, newMenuInfos, 0, menuInfos.length);
        newMenuInfos[newMenuInfos.length - 1] = menuInfo;
        menuInfos = newMenuInfos;
        Arrays.sort(menuInfos, Comparator.comparingInt(MenuInfo::getOrder));
    }

    public void addModelInfo(ModelInfo modelInfo) {
        ModelInfo[] newModelInfos = new ModelInfo[modelInfos.length + 1];
        System.arraycopy(modelInfos, 0, newModelInfos, 0, modelInfos.length);
        newModelInfos[newModelInfos.length - 1] = modelInfo;
        modelInfos = newModelInfos;
        Arrays.sort(modelInfos, Comparator.comparingInt(ModelInfo::getOrder));
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

    public ModelInfo[] getModelInfos() {
        return modelInfos;
    }

    public MenuInfo[] getMenuInfos() {
        return menuInfos;
    }
}
