package qingzhou.app.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.AppStub;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceRegister;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Arrays;

public class Controller extends ServiceRegister<AppManager> {
    private FrameworkContext frameworkContext;
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
        this.frameworkContext = frameworkContext;
        this.appManager = new AppManagerImpl(frameworkContext);
        FileManager fileManager = frameworkContext.getFileManager();

        if (frameworkContext.isMaster()) {
            File masterApp = FileUtil.newFile(fileManager.getLib(), "sysapp", FrameworkContext.SYS_APP_MASTER);
            installApp(masterApp);
        }

        File nodeApp = FileUtil.newFile(fileManager.getLib(), "sysapp", FrameworkContext.SYS_APP_NODE_AGENT);
        installApp(nodeApp);

        File[] files = new File(fileManager.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                installApp(file);
            }
        }
    }

    private void installApp(File app) throws Exception {
        String name = app.getName();
        appManager.installApp(name, app);

        if (frameworkContext.isMaster()) {
            ConsoleContext consoleContext = appManager.getApp(name).getAppContext().getConsoleContext();

            //todo： AppStubManager 不应该是 app 容器的内容，从 FrameworkContext 根上移动走 ？
            frameworkContext.getAppStubManager().registerAppStub(name, new AppStub() {
                @Override
                public ModelManager getModelManager() {
                    return consoleContext.getModelManager();
                }

                @Override
                public String getI18N(Lang lang, String key, Object... args) {
                    return consoleContext.getI18N(lang, key, args);
                }

                @Override
                public MenuInfo getMenuInfo(String menuName) {
                    return consoleContext.getMenuInfo(menuName);
                }

                @Override
                public String getEntryModel() {
                    return consoleContext.getEntryModel();
                }
            });
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
