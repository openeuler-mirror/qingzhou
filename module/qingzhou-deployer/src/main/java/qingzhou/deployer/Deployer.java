package qingzhou.deployer;

import java.io.File;
import java.util.List;

public interface Deployer {
    void addAppListener(AppListener appListener);

    void installApp(File appDir) throws Exception;

    void unInstallApp(String appName) throws Exception;

    List<String> getAllApp();

    App getApp(String appName);
}