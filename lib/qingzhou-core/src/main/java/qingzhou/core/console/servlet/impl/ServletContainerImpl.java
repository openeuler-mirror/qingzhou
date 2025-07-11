package qingzhou.core.console.servlet.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.tomcat.util.modeler.Registry;
import qingzhou.core.console.servlet.ServletContainer;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.impl.Controller;

public class ServletContainerImpl implements ServletContainer {
    private Tomcat tomcat;

    @Override
    public void start(int port, File baseDir, Properties properties) throws Throwable {
        // doInThreadContextClassLoader 为了 接管 tomcat 的日志系统
        Utils.doInThreadContextClassLoader(TomcatLogDelegate.class.getClassLoader(), () -> {
            Registry.disableRegistry(); // 禁用 tomcat 的 Jmx MBean
            tomcat = new Tomcat();
            tomcat.setBaseDir(baseDir.getAbsolutePath());
            tomcat.setPort(port); // 设置默认连接器端口
            tomcat.getHost().setParentClassLoader(Tomcat.class.getClassLoader());// 应用需要依赖 tomcat 里面的 javax.servlet api
            Connector connector = tomcat.getConnector();// 建立连接器

            int setMaxPostSize = 100 * 1024 * 1024; // 修改默认值
            if (properties != null) {
                String maxPostSize = properties.getProperty("maxPostSize");
                if (maxPostSize != null) {
                    setMaxPostSize = Integer.parseInt(maxPostSize);
                }
                if (Boolean.parseBoolean(properties.getProperty("enabledRemoteIpValve"))) { // 云帆用
                    RemoteIpValve remoteIpValve = new RemoteIpValve();
                    remoteIpValve.setRemoteIpHeader("X-Forwarded-For");
                    remoteIpValve.setProtocolHeader("X-Forwarded-Proto");
                    remoteIpValve.setHostHeader("X-Forwarded-Host");
                    remoteIpValve.setPortHeader("X-Forwarded-Port");
                    tomcat.getEngine().getPipeline().addValve(remoteIpValve);
                }
            }
            // 设置最大文件上传的大小
            connector.setMaxPostSize(setMaxPostSize);

            tomcat.start(); // 启动服务器
        });
    }

    @Override
    public void addWebapp(String contextPath, String docBase, Properties properties) {
        Context context = tomcat.addWebapp(contextPath, docBase);// 指定部署应用的信息
        // 添加Multipart配置
        if (context instanceof StandardContext) {
            context.setAllowCasualMultipartParsing(true);
        }
        context.setRequestCharacterEncoding(StandardCharsets.UTF_8.name());
        context.setResponseCharacterEncoding(StandardCharsets.UTF_8.name());

        addWebResources(context, properties);
    }

    private void addWebResources(Context context, Properties properties) {
        String webResources = properties.getProperty("webResources");
        if (webResources != null) {
            for (String alt : webResources.trim().split(",")) {
                String trim = alt.trim();
                if (trim.isEmpty()) continue;

                String mount = "/";
                String dir = trim;
                int i = trim.indexOf("=");
                if (i > 0) {
                    mount = trim.substring(0, i);
                    dir = trim.substring(i + 1);
                }
                if (Utils.isBlank(mount)) {
                    mount = "/";
                }
                File appsDir = new File(dir);
                if (!appsDir.exists()) {
                    FileUtil.mkdirs(appsDir);
                }

                WebResourceRoot root = context.getResources();
                root.addPreResources(new DirResourceSet(root, mount, dir, "/"));
            }
        }
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
