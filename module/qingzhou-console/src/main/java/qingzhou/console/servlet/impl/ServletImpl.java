package qingzhou.console.servlet.impl;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import qingzhou.console.servlet.ServletProcessor;
import qingzhou.console.servlet.ServletService;
import qingzhou.framework.util.ClassLoaderUtil;
import qingzhou.framework.pattern.Callback;

import java.nio.charset.StandardCharsets;

public class ServletImpl implements ServletService {
    private Tomcat tomcat;

    @Override
    public void start(int port, String baseDir) throws Exception {
        ClassLoaderUtil.runUnderClassLoader((Callback<Void, Void>) args -> {
            tomcat = new Tomcat();
            if (null != baseDir && !"".equals(baseDir)) {
                tomcat.setBaseDir(baseDir);
            }
            tomcat.setPort(port); // 设置默认连接器端口
            tomcat.getHost().setParentClassLoader(Tomcat.class.getClassLoader());// 应用需要依赖 tomcat 里面的 javax.servlet api
            Connector connector = tomcat.getConnector();// 建立连接器
            // 设置最大文件上传的大小
            connector.setMaxPostSize(104857600);// 100 MB
            tomcat.start(); // 启动服务器
            return null;
        }, ServletImpl.class.getClassLoader());
    }

    @Override
    public void addSingleServletWebapp(String contextPath, String mapping, String docBase, ServletProcessor processor) {
        Host host = tomcat.getHost();
        host.addChild(new SingleServletContext(contextPath, mapping, docBase, new AdapterServlet(processor)));
    }

    @Override
    public void addWebapp(String contextPath, String docBase) {
        Context context = tomcat.addWebapp(contextPath, docBase);// 指定部署应用的信息
        // 添加Multipart配置
        if(context instanceof StandardContext){
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
            e.printStackTrace();
        }

        try {
            tomcat.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
