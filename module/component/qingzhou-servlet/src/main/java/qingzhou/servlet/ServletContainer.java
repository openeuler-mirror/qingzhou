package qingzhou.servlet;

public interface ServletContainer {
    void start(int port, String cacheDir) throws Exception;

    void addWebapp(String contextPath, String docBase);

    void removeApp(String contextPath);

    void stop();
}
