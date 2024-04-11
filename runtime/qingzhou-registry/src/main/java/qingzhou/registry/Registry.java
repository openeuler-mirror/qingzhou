package qingzhou.registry;

public interface Registry {
    void register(InstanceInfo instanceInfo);

    void register(AppInfo instanceInfo);

    String[] getAllInstanceIds();

    InstanceInfo getInstanceInfo(String id);

    String[] getAllAppIds();

    AppInfo getAppInfo(String id);
}
