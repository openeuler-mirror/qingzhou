package qingzhou.servlet.impl;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.modeler.Registry;
import qingzhou.servlet.ServletContainer;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class ServletContainerImpl implements ServletContainer {
    private Tomcat tomcat;

    @Override
    public void start(int port, File baseDir) throws Exception {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 为了 接管 tomcat 的日志系统
            Thread.currentThread().setContextClassLoader(TomcatLogDelegate.class.getClassLoader());

            Registry.disableRegistry(); // 禁用 tomcat 的 Jmx MBean
            tomcat = new Tomcat();
            tomcat.setBaseDir(baseDir.getAbsolutePath());
            tomcat.setPort(port); // 设置默认连接器端口
            tomcat.getHost().setParentClassLoader(Tomcat.class.getClassLoader());// 应用需要依赖 tomcat 里面的 javax.servlet api
            Connector connector = tomcat.getConnector();// 建立连接器
            // 设置最大文件上传的大小
            connector.setMaxPostSize(104857600);// 100 MB
            tomcat.start(); // 启动服务器
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    @Override
    public void addWebapp(String contextPath, String docBase) {
        Context context = tomcat.addWebapp(contextPath, docBase);// 指定部署应用的信息
        // 添加Multipart配置
        if (context instanceof StandardContext) {
            context.setAllowCasualMultipartParsing(true);
        }
        context.setRequestCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    @Override
    public void removeApp(String contextPath) {
        Host host = tomcat.getHost();
        Context found = null;
        for (Container child : host.findChildren()) {
            Context context = (Context) child;
            if (context.getPath().equals(contextPath)) {
                found = context;
                break;
            }
        }
        if (found != null) {
            host.removeChild(found);
        }
    }

    @Override
    public void stop() {
        try {
            tomcat.stop();
        } catch (Exception e) {
            Controller.logger.warn(e.getMessage(), e);
        }

        try {
            tomcat.destroy();
        } catch (Exception e) {
            Controller.logger.warn(e.getMessage(), e);
        }
    }
}
