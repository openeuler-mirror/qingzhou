package qingzhou.deployer.impl;

import qingzhou.crypto.CryptoService;
import qingzhou.deployer.Deployer;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.Arrays;

@Module
public class Controller implements ModuleActivator {
    @Service
    private Logger logger;
    @Service
    private CryptoService cryptoService;

    private Deployer deployer;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        deployer = new DeployerImpl(cryptoService, moduleContext);

        moduleContext.registerService(Deployer.class, deployer);

        File masterApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "master");
        if (masterApp.exists()) {
            deployer.installApp(masterApp);
        }

        File instanceApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "instance");
        if (instanceApp.exists()) {
            deployer.installApp(instanceApp);
        }

        File[] files = FileUtil.newFile(moduleContext.getInstanceDir(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                deployer.installApp(file);
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
