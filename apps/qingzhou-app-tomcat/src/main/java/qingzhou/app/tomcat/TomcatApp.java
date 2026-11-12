package qingzhou.app.tomcat;


import qingzhou.api.AppContext;
import qingzhou.api.QingzhouApp;
import qingzhou.api.annotations.App;
import qingzhou.api.annotations.I18n;

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
@I18n(name = {"内存", "en:Memory"}, code = "Memory")
@I18n(name = {"进程", "en:Process"}, code = "Process")
@I18n(name = {"系统资源", "en:OS Resources"}, code = "Os")
@I18n(name = {"文件句柄", "en:File Descriptors"}, code = "File")
@I18n(name = {"垃圾回收", "en:GC"}, code = "GC")
public class TomcatApp implements QingzhouApp {
    @Override
    public void start(AppContext appContext) {
        // attach 失败时统一转 response.error()，避免冒泡为 HTTP 500。
        // Dashboard 首页需渲染"未启动"状态，不走此拦截，由 Dashboard.show() 自行 try-catch。
        appContext.addActionFilter((request, chain) -> {
            String model = request.getModel();
            if ("dashboard".equals(model)) {
                chain.doFilter();
                return;
            }
            try {
                chain.doFilter();
            } catch (TomcatNotRunningException e) {
                request.getResponse()
                        .success(false)
                        .message("Tomcat 未启动，请先启动 Tomcat 后重试")
                        .messageLevel(qingzhou.api.Response.MessageLevel.warn);
            }
        });
    }

    @Override
    public void stop() {
    }
}
