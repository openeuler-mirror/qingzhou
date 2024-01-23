package qingzhou.framework;

import java.io.File;

public interface AppDeployer {
    void installApp(String name, File app) throws Exception;

    void unInstallApp(String name) throws Exception;
}
