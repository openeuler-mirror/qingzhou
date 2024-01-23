package qingzhou.framework;

import java.io.File;

public interface AppDeployer extends InternalService {
    void installApp(String name, File app) throws Exception;

    void unInstallApp(String name) throws Exception;
}
