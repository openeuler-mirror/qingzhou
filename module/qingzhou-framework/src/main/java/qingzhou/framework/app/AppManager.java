package qingzhou.framework.app;

import qingzhou.framework.service.InternalService;

import java.io.File;
import java.util.Set;

public interface AppManager extends InternalService {
    void installApp(File app) throws Exception;

    void unInstallApp(String name) throws Exception;

    Set<String> getApps();

    App getApp(String name);
}
