package qingzhou.registry;

import java.util.Collection;

public class ModelInfo {
    public final Collection<ModelFieldInfo> modelFieldInfos;
    public final Collection<ModelActionInfo> modelActionInfos;

    public ModelInfo(Collection<ModelFieldInfo> modelFieldInfos, Collection<ModelActionInfo> modelActionInfos) {
        this.modelFieldInfos = modelFieldInfos;
        this.modelActionInfos = modelActionInfos;
    }
}
