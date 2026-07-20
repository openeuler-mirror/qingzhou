package qingzhou.app.tomcat;


import qingzhou.api.*;

@App(icon = "/icons/logo.png",
        name = {"Tomcat管理", "en:Tomcat Manager"},
        info = {"Tomcat管理应用，提供连接器配置、应用部署、线程池监控、日志查看等功能。", "en:Tomcat management application with connector, deployment, thread pool, and log features."})
public class TomcatApp implements QingzhouApp{
    @Override
    public void start(AppContext appContext) {
    }
    @Override
    public void stop() {
    }
}
