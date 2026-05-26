package qingzhou.agent.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Qingzhou Agent - Java Agent entry point for embedding Qingzhou runtime into JVM.
 *
 * <p>Usage:
 * <pre>
 * java -javaagent:qingzhou-agent.jar=path/to/qingzhou-agent.properties -jar your-application.jar
 * </pre>
 *
 * <p>The agent reads annotation.json files from the apps directory (provided by application developers),
 * loads app JARs via URLClassLoader, starts an embedded HTTP server, and handles heartbeat/registration
 * with the registry center.
 */
public class QingzhouAgent {
    private static volatile ServiceContainer container;

    /**
     * JVM startup agent entry point.
     * Called before the application's main method.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[QingzhouAgent] Starting...");
        try {
            Properties config = loadConfig(agentArgs);
            container = new ServiceContainer(config);
            container.initialize();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (container != null) {
                    container.shutdown();
                }
            }));

            System.out.println("[QingzhouAgent] Started successfully");
        } catch (Exception e) {
            System.err.println("[QingzhouAgent] Failed to start");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Dynamic attach agent entry point.
     * Called when attaching to a running JVM.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }

    private static Properties loadConfig(String agentArgs) {
        Properties props = new Properties();

        // Priority 1: agentArgs points to a config file path
        if (agentArgs != null && !agentArgs.isEmpty()) {
            String configPath = agentArgs.split("=", 2)[0];
            File configFile = new File(configPath);
            if (configFile.exists() && configFile.isFile()) {
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
                    return props;
                } catch (Exception e) {
                    System.err.println("[QingzhouAgent] Failed to load config file: " + configPath);
                    e.printStackTrace(System.err);
                }
            }
        }

        // Priority 2: qingzhou.agent.config system property
        String sysConfig = System.getProperty("qingzhou.agent.config");
        if (sysConfig != null && !sysConfig.isEmpty()) {
            File sysConfigFile = new File(sysConfig);
            if (sysConfigFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(sysConfigFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
                    return props;
                } catch (Exception e) {
                    System.err.println("[QingzhouAgent] Failed to load config from qingzhou.agent.config: " + sysConfig);
                }
            }
        }

        // Priority 3: Default config from classpath or work directory
        File defaultFile = new File("qingzhou-agent.properties");
        if (defaultFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(defaultFile), StandardCharsets.UTF_8)) {
                props.load(reader);
                return props;
            } catch (Exception e) {
                System.err.println("[QingzhouAgent] Failed to load default config");
            }
        }

        // Priority 4: Individual system properties (qingzhou.agent.*)
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("qingzhou.agent.")) {
                props.setProperty(key, System.getProperty(key));
            }
        }

        // Set defaults
        if (!props.containsKey("qingzhou.agent.port")) {
            props.setProperty("qingzhou.agent.port", "8080");
        }
        if (!props.containsKey("qingzhou.agent.apps.dir")) {
            props.setProperty("qingzhou.agent.apps.dir", "./apps");
        }
        if (!props.containsKey("qingzhou.agent.instance.dir")) {
            props.setProperty("qingzhou.agent.instance.dir", "./instance");
        }
        if (!props.containsKey("qingzhou.agent.heartbeat.interval")) {
            props.setProperty("qingzhou.agent.heartbeat.interval", "60");
        }

        return props;
    }
}