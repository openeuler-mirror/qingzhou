package qingzhou.app.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceRegister;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Arrays;

public class Controller extends ServiceRegister<AppManager> {
    private AppManagerImpl appManager;

    @Override
    protected Class<AppManager> serviceType() {
        return AppManager.class;
    }

    @Override
    protected AppManager serviceObject() {
        return appManager;
    }

    @Override
    protected void startService(FrameworkContext frameworkContext) throws Exception {
        this.appManager = new AppManagerImpl(frameworkContext);
        FileManager fileManager = frameworkContext.getFileManager();

        File nodeApp = FileUtil.newFile(fileManager.getLib(), "sysapp", FrameworkContext.SYS_APP_NODE_AGENT);
        appManager.installApp(nodeApp);

        File[] files = new File(fileManager.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                appManager.installApp(file);
            }
        }
    }

    @Override
    protected void stopService() {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                appManager.unInstallApp(appName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
