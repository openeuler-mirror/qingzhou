package qingzhou.registry;

public class AppInfo {
    public String name;
    public ModelInfo[] modelInfos;

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
