package qingzhou.servlet;

import java.io.File;
import java.util.Properties;

public interface ServletContainer {
    void start(int port, File baseDir, Properties properties) throws Exception;

    void addWebapp(String contextPath, String docBase);

    void removeApp(String contextPath);

    void stop();
}
