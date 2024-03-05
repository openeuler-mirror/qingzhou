package qingzhou.app;

import qingzhou.app.mbean.impl.ServerJmx;
import qingzhou.bootstrap.main.service.ServiceRegister;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.util.FileUtil;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class Controller extends ServiceRegister<AppManager> {
    public static Logger logger;
    public static AppManager appManager;
    public static String SERVER_M_BEAN_NAME = "QingZhou:name=server";

    @Override
    public Class<AppManager> serviceType() {
        return AppManager.class;
    }

    @Override
    protected AppManager serviceObject() {
        return appManager;
    }

    @Override
    protected void startService() throws Exception {
        super.startService();

        Controller.logger = frameworkContext.getServiceManager().getService(Logger.class);
        appManager = new AppManagerImpl(frameworkContext);

        File masterApp = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", App.SYS_APP_MASTER);
        appManager.installApp(masterApp);

        File nodeAgentApp = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", App.SYS_APP_NODE_AGENT);
        appManager.installApp(nodeAgentApp);

        File[] files = FileUtil.newFile(frameworkContext.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                appManager.installApp(file);
            }
        }
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(SERVER_M_BEAN_NAME);
        mBeanServer.registerMBean(new ServerJmx(), name);
    }

    @Override
    protected void stopService() {
        super.stopService();

        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                appManager.unInstallApp(appName);
            } catch (Exception e) {
                logger.warn("failed to stop app: " + appName, e);
            }
        });
    }
}