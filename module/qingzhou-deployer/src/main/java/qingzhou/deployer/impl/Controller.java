package qingzhou.deployer.impl;

import qingzhou.config.Config;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;

import java.io.File;
import java.util.Arrays;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Logger logger;

    @Service
    private Registry registry;

    @Service
    private Config config;

    @Service
    private Json json;

    static Deployer deployer;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        deployer = new DeployerImpl(moduleContext);

        moduleContext.registerService(Deployer.class, deployer);

        File masterApp = Utils.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", DeployerConstants.MASTER_APP_NAME);
        if (masterApp.exists()) {
            deployer.installApp(masterApp);
        }

        File instanceApp = Utils.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", DeployerConstants.INSTANCE_APP_NAME);
        if (instanceApp.exists()) {
            deployer.installApp(instanceApp);
        }

        File[] files = Utils.newFile(moduleContext.getInstanceDir(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                try {
                    deployer.installApp(file);
                } catch(Exception e) {
                    logger.error("failed to install app " + file.getName(), e);
                }
            }
        }
    }

    @Override
    public void stop() {
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
