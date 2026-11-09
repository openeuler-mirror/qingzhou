package qingzhou.app.tomcat;


import qingzhou.api.*;

@App(icon = "/icons/logo.png", roles = "admin",
        name = {"Tomcat管理", "en:Tomcat Manager"},
        info = {"Tomcat管理应用，提供连接器配置、应用部署、线程池监控、日志查看等功能。", "en:Tomcat management application with connector, deployment, thread pool, and log features."})

@I18n(name = {"基础信息", "en:Basic Info"}, code = "Basic")
@I18n(name = {"会话", "en:Session"}, code = "Session")
@I18n(name = {"请求", "en:Request"}, code = "Request")
@I18n(name = {"流量", "en:Traffic"}, code = "Traffic")
@I18n(name = {"线程", "en:Thread"}, code = "Thread")
@I18n(name = {"连接", "en:Connection"}, code = "Connection")
@I18n(name = {"响应耗时", "en:Latency"}, code = "Latency")
@I18n(name = {"虚拟机", "en:JVM"}, code = "Jvm")
@I18n(name = {"内存", "en:Memory"}, code = "Memory")
public class TomcatApp implements QingzhouApp{
    @Override
    public void start(AppContext appContext) {
    }
    @Override
    public void stop() {
    }
}
