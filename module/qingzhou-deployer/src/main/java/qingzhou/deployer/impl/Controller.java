package qingzhou.deployer.impl;

import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.Service;
import qingzhou.engine.util.FileUtil;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.Registry;
import qingzhou.servlet.ServletService;
import qingzhou.ssh.SSHService;
import qingzhou.uml.Uml;

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

    @Service
    private QrGenerator qrGenerator;

    @Service
    private SSHService sshService;

    @Service
    private Http http;

    @Service
    private ServletService servletService;

    @Service
    private CryptoService cryptoService;

    @Service
    private Uml uml;
    private DeployerImpl deployer;

    @Override
    public void start(ModuleContext moduleContext) throws Exception {
        deployer = new DeployerImpl(moduleContext, registry, logger);
        deployer.appsBase = FileUtil.newFile(moduleContext.getInstanceDir(), "apps");

        moduleContext.registerService(Deployer.class, deployer);
        moduleContext.registerService(ActionInvoker.class, new ActionInvokerImpl(deployer, registry, json, cryptoService, http, logger, config));

        deployer.setLoaderPolicy(new DeployerImpl.LoaderPolicy() {
            @Override
            public ClassLoader getClassLoader() {
                return QingzhouSystemApp.class.getClassLoader();
            }

            @Override
            public File[] getAdditionalLib() {
                return null;
            }
        });
        File systemApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", DeployerConstants.APP_SYSTEM);
        deployer.installApp(systemApp);

        deployer.setLoaderPolicy(new DeployerImpl.LoaderPolicy() {
            final File commonApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common");

            @Override
            public ClassLoader getClassLoader() {
                return moduleContext.getApiLoader();
            }

            @Override
            public File[] getAdditionalLib() {
                return commonApp.listFiles();
            }
        });
        File[] files = deployer.appsBase.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                try {
                    deployer.installApp(file);
                } catch (Exception e) {
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
