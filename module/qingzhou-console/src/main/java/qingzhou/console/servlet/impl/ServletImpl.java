package qingzhou.console.servlet.impl;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.startup.Tomcat;
import qingzhou.console.servlet.ServletProcessor;
import qingzhou.console.servlet.ServletService;

import java.nio.charset.StandardCharsets;

public class ServletImpl implements ServletService {
    private Tomcat tomcat;

    @Override
    public void start(int port, String baseDir) throws Exception {
        tomcat = new Tomcat();
        if (null != baseDir && !"".equals(baseDir)) {
            tomcat.setBaseDir(baseDir);
        }
        tomcat.setPort(port); // 设置默认连接器端口
        tomcat.getHost().setParentClassLoader(Tomcat.class.getClassLoader());// 应用需要依赖 tomcat 里面的 javax.servlet api
        tomcat.getConnector(); // 建立连接器
        tomcat.start(); // 启动服务器
    }

    @Override
    public void addSingleServletWebapp(String contextPath, String mapping, String docBase, ServletProcessor processor) {
        Host host = tomcat.getHost();
        host.addChild(new SingleServletContext(contextPath, mapping, docBase, new AdapterServlet(processor)));
    }

    @Override
    public void addWebapp(String contextPath, String docBase) {
        Context context = tomcat.addWebapp(contextPath, docBase);// 指定部署应用的信息
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
            e.printStackTrace();
        }

        try {
            tomcat.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
