package qingzhou.agent.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import qingzhou.agent.embedded.driver.AppLoader;
import qingzhou.agent.embedded.driver.ServiceContainerProvider;
import qingzhou.agent.embedded.handler.AgentHttpHandler;
import qingzhou.agent.embedded.heartbeat.HeartbeatService;
import qingzhou.agent.embedded.i18n.I18nService;
import qingzhou.agent.embedded.i18n.I18nServiceImpl;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.impl.HttpClientImpl;
import qingzhou.http.server.HttpServer;
import qingzhou.http.server.impl.HttpServerImpl;
import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.Logger;
import qingzhou.logger.impl.LoggerImpl;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.EmbeddedRegistry;

public class ServiceContainer {
    private final Properties config;
    private final Logger logger;
    private final List<AppLoader> appLoaders = new ArrayList<>();

    private HttpServerImpl httpServer;
    private HeartbeatService heartbeatService;

    public ServiceContainer(Properties config) {
        this.config = config;
        this.logger = new LoggerImpl();
        ServiceContainerProvider.registerService(Logger.class, logger);
        // Also register directly for consistent access
        ServiceContainerProvider.registerService(Crypto.class, new CryptoImpl());
        ServiceContainerProvider.registerService(Json.class, new JsonImpl());
        ServiceContainerProvider.registerService(HttpClient.class, new HttpClientImpl());
        ServiceContainerProvider.registerService(I18nService.class, new I18nServiceImpl());
    }

    public void initialize() throws Exception {
        logger.info("[QingzhouAgent] Initializing...");

        // 1. Get basic services (already registered in constructor)
        JsonImpl json = (JsonImpl) ServiceContainerProvider.getService(Json.class);
        CryptoImpl crypto = (CryptoImpl) ServiceContainerProvider.getService(Crypto.class);
        HttpClient httpClient = ServiceContainerProvider.getService(HttpClient.class);

        // 2. Create registry
        EmbeddedRegistry registry = new EmbeddedRegistry(logger, json, crypto);
        ServiceContainerProvider.registerService(Registry.class, registry);

        // 3. Create HTTP server
        int port = Integer.parseInt(config.getProperty("qingzhou.agent.port", "8080"));
        httpServer = new HttpServerImpl(port);

        // 4. Create agent HTTP handler
        AgentHttpHandler agentHandler = new AgentHttpHandler(logger, json, registry, crypto);
        httpServer.registerHttpHandler(agentHandler, "/agent");

        // 5. Load apps
        String appsDir = config.getProperty("qingzhou.agent.apps.dir", "./apps");
        File appsDirFile = new File(appsDir);
        if (appsDirFile.exists() && appsDirFile.isDirectory()) {
            scanAndLoadApps(appsDirFile, registry, json, crypto);
        } else {
            logger.warn("Apps directory not found: " + appsDirFile.getAbsolutePath());
        }

        // 6. Create heartbeat service
        String registryUrl = config.getProperty("qingzhou.agent.registry.url", "http://localhost:8088");
        String publicKey = config.getProperty("qingzhou.agent.registry.publicKey", "");
        int interval = Integer.parseInt(config.getProperty("qingzhou.agent.heartbeat.interval", "60"));
        File instanceDir = new File(config.getProperty("qingzhou.agent.instance.dir", "./instance"));
        instanceDir.mkdirs();

        heartbeatService = new HeartbeatService(port, registryUrl, publicKey,
                interval, httpClient, crypto, registry, json, logger, instanceDir);

        // 7. Start HTTP server
        httpServer.start();
        logger.info("[QingzhouAgent] HTTP Server started on port " + port);

        // 8. Set agent handler instance info
        agentHandler.setInstanceInfo(heartbeatService.getInstanceInfo());

        // 9. Start heartbeat
        if (publicKey != null && !publicKey.isEmpty()) {
            heartbeatService.start();
            logger.info("[QingzhouAgent] Heartbeat started");
        } else {
            logger.warn("[QingzhouAgent] No publicKey configured, heartbeat disabled");
        }

        // 10. Start app lifecycle monitors
        for (AppLoader loader : appLoaders) {
            loader.start();
        }

        logger.info("[QingzhouAgent] Initialization complete");
    }

    private void scanAndLoadApps(File appsDir, EmbeddedRegistry registry, Json json, Crypto crypto) {
        File qzInfDir = new File(appsDir, "QZ-INF");
        File annotationJsonFile = new File(qzInfDir, "annotation.json");

        if (!annotationJsonFile.exists()) {
            logger.warn("annotation.json not found at: " + annotationJsonFile.getAbsolutePath());
            // Also search for subdirectories with apps
            File[] subDirs = appsDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    File subAnnotationFile = new File(new File(subDir, "QZ-INF"), "annotation.json");
                    if (subAnnotationFile.exists()) {
                        loadApp(registry, json, subAnnotationFile, subDir);
                    }
                }
            }
            return;
        }

        loadApp(registry, json, annotationJsonFile, appsDir);
    }

    private void loadApp(EmbeddedRegistry registry, Json json, File annotationJsonFile, File appDir) {
        try {
            // Read annotation.json
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(annotationJsonFile), StandardCharsets.UTF_8)) {
                char[] buf = new char[1024];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
            }

            AppMeta appMeta = json.fromJson(sb.toString(), AppMeta.class);
            String appCode = appMeta.getApp().code;

            // Find the app JAR in the same directory
            File appJar = findAppJar(appDir, appCode);
            if (appJar == null) {
                logger.warn("App JAR not found for: " + appCode + " in " + appDir.getAbsolutePath());
                return;
            }

            File instanceDir = new File(config.getProperty("qingzhou.agent.instance.dir", "./instance"));

            AppLoader loader = new AppLoader(appJar, appMeta, instanceDir, logger);
            registry.registerLocalApp(appCode, loader.getAppStub());
            appLoaders.add(loader);

            logger.info("App loaded: " + appCode + " from " + appJar.getName());
        } catch (Exception e) {
            logger.error("Failed to load app from: " + annotationJsonFile.getAbsolutePath(), e);
        }
    }

    private File findAppJar(File dir, String appCode) {
        // Search for JAR files in the directory
        File[] jarFiles = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                // Simple heuristic: the jar name contains the app code
                if (jar.getName().contains(appCode)) {
                    return jar;
                }
            }
            // If no match, return first JAR
            if (jarFiles.length > 0) return jarFiles[0];
        }
        return null;
    }

    public void shutdown() {
        logger.info("[QingzhouAgent] Shutting down...");

        if (heartbeatService != null) {
            heartbeatService.stop();
        }

        for (AppLoader loader : appLoaders) {
            try {
                loader.close();
            } catch (Exception e) {
                logger.warn("Error stopping app: " + loader.getAppCode(), e);
            }
        }

        if (httpServer != null) {
            httpServer.stop();
        }

        ServiceContainerProvider.clear();
        logger.info("[QingzhouAgent] Shutdown complete");
    }
}