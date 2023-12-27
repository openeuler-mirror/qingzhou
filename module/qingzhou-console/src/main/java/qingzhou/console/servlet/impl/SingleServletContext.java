package qingzhou.console.servlet.impl;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class SingleServletContext extends StandardContext {
    private String contextPath;
    private String mapping;
    private HttpServlet servlet;

    public SingleServletContext() {
        setJarScanner(new EmptyScanner());

        addLifecycleListener(event -> {
            Context context = (Context) event.getLifecycle();
            if (event.getType().equals(Lifecycle.START_EVENT) || event.getType().equals(Lifecycle.BEFORE_START_EVENT) || event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                context.setConfigured(true);
            }
        });
    }

    public SingleServletContext(String contextPath, String mapping, String docBase,HttpServlet servlet) {
        this();

        this.contextPath = contextPath;
        this.mapping = mapping;
        this.servlet = servlet;

        setDocBase(docBase);
        setParentClassLoader(Tomcat.class.getClassLoader());// 应用需要依赖 tomcat 里面的 javax.servlet api
        setPath(contextPath);
        setSessionTimeout(1);
        setRequestCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();

        if (contextPath != null) {
            final Wrapper servlet = createWrapper();
            String name = this.servlet.getClass().getSimpleName();
            servlet.setName(name);
            servlet.setServletClass(this.servlet.getClass().getName());
            servlet.setServlet(this.servlet);
            servlet.setMultipartConfigElement(new MultipartConfigElement(""));
            addChild(servlet);
            addServletMappingDecoded(mapping, name);
        }
    }

    private static class EmptyScanner implements JarScanner {
        private JarScanFilter scanner;

        @Override
        public void scan(final JarScanType scanType, final ServletContext context,
                         final JarScannerCallback callback) {
            // no-op
        }

        @Override
        public JarScanFilter getJarScanFilter() {
            return scanner;
        }

        @Override
        public void setJarScanFilter(final JarScanFilter jarScanFilter) {
            this.scanner = jarScanFilter;
        }
    }

}
