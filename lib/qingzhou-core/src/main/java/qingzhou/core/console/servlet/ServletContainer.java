package qingzhou.core.console.servlet;

import java.io.File;
import java.util.Properties;

public interface ServletContainer {
    void start(int port, File baseDir, Properties properties) throws Throwable;

    void addWebapp(String contextPath, String docBase, Properties properties);

    void removeApp(String contextPath);

    void stop();
}
