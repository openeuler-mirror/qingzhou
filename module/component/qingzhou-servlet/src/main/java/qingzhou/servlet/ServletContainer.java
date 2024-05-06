package qingzhou.servlet;

import java.io.File;

public interface ServletContainer {
    void start(int port, File baseDir) throws Exception;

    void addWebapp(String contextPath, String docBase);

    void removeApp(String contextPath);

    void stop();
}
