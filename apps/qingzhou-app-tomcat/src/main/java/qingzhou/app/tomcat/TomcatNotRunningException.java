package qingzhou.app.tomcat;

/**
 * attach 失败时抛出：目标安装路径下没有运行中、catalina.home 匹配的 Tomcat 进程。
 * 继承 Error 而非 RuntimeException：TomcatJmx.read() 的 catch(Exception) 重试会穿透它，
 * 模型层 loadXxx() 的 catch(Exception) 包装也会穿透它，最终被 TomcatApp 注册的 ActionFilter 捕获转 response.error()。
 */
class TomcatNotRunningException extends Error {
    TomcatNotRunningException(String tomcatHome) {
        super("未找到与安装路径匹配的运行中 Tomcat: " + tomcatHome);
    }
}
