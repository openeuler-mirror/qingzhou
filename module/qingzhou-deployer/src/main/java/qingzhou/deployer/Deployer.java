package qingzhou.deployer;

import qingzhou.registry.InstanceInfo;

import java.io.File;
import java.util.Collection;

public interface Deployer {
    void installApp(File app) throws Exception;

    void unInstallApp(String name) throws Exception;

    Collection<String> getAllApp();

    App getApp(String name);

    InstanceInfo getInstanceInfo();
}
