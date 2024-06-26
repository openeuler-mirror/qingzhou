package qingzhou.deployer;

import java.io.File;
import java.util.Collection;

public interface Deployer {
    void installApp(File appDir) throws Exception;

    void unInstallApp(String name) throws Exception;

    Collection<String> getAllApp();

    App getApp(String name);
}
