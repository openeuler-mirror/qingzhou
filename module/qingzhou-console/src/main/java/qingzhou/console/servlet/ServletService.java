package qingzhou.console.servlet;


public interface ServletService {
    void start(int port, String baseDir) throws Exception;

    void addSingleServletWebapp(String contextPath, String mapping, String docBase, ServletProcessor processor);

    void addWebapp(String contextPath, String docBase);

    void removeApp(String contextPath);

    void stop();
}
