package qingzhou.registry;

public class ModelInfo {
    public ModelFieldInfo[] modelFieldInfos;
    public ModelActionInfo[] modelActionInfos;

    public ModelFieldInfo[] getModelFieldInfos() {
        return modelFieldInfos;
    }

    public void setModelFieldInfos(ModelFieldInfo[] modelFieldInfos) {
        this.modelFieldInfos = modelFieldInfos;
    }

    public ModelActionInfo[] getModelActionInfos() {
        return modelActionInfos;
    }

    public void setModelActionInfos(ModelActionInfo[] modelActionInfos) {
        this.modelActionInfos = modelActionInfos;
    }
}
