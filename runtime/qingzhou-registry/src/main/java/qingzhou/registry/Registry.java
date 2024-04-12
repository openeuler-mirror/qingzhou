package qingzhou.registry;

import java.util.Collection;

public interface Registry {
    void register(String registrationData);

    Collection<String> getAllInstanceId();

    InstanceInfo getInstanceInfo(String id);
}
