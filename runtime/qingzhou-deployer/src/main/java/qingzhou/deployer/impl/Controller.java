package qingzhou.deployer.impl;

import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.engine.ServiceRegister;
import qingzhou.engine.util.FileUtil;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.Arrays;

public class Controller extends ServiceRegister<Deployer> {
    public static Logger logger;
    public static Deployer deployer;

    @Override
    public Class<Deployer> serviceType() {
        return Deployer.class;
    }

    @Override
    protected Deployer serviceObject() {
        return deployer;
    }

    @Override
    protected void startService() throws Exception {
        super.startService();

        Controller.logger = moduleContext.getService(Logger.class);
        deployer = new DeployerImpl(moduleContext);

        File masterApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", App.SYS_APP_MASTER);
        deployer.installApp(masterApp);

        File nodeAgentApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", App.SYS_APP_NODE_AGENT);
        deployer.installApp(nodeAgentApp);

        File[] files = FileUtil.newFile(moduleContext.getInstanceDir(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                deployer.installApp(file);
            }
        }
    }

    @Override
    protected void stopService() {
        super.stopService();

        String[] apps = deployer.getAllApp().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                deployer.unInstallApp(appName);
            } catch (Exception e) {
                logger.warn("failed to stop app: " + appName, e);
            }
        });
    }
}
