package qingzhou.registry;

import java.util.Collection;

public class AppInfo {
    public final String name;
    public final String instanceId;
    public final Collection<ModelInfo> modelInfos;

    public AppInfo(String name, String instanceId, Collection<ModelInfo> modelInfos) {
        this.name = name;
        this.instanceId = instanceId;
        this.modelInfos = modelInfos;
    }
}
