package qingzhou.console.controller;

import qingzhou.api.console.ConsoleContext;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.doc.Manual;
import qingzhou.console.i18n.I18nFilter;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.console.security.JspInterceptor;
import qingzhou.console.security.TrustedIPChecker;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.pattern.FilterPattern;
import qingzhou.framework.pattern.Process;
import qingzhou.framework.pattern.ProcessSequence;

import javax.servlet.FilterChain;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    // 可添加自定义监听器，监听 console 的启动和停止事件
    public static final ProcessSequence COMPONENT_SEQUENCE = new ProcessSequence(
            new InitConsoleMenu()
    );
    private static final List<Runnable> shutdownHooks = new ArrayList<>();

    public static void addShutdownHook(Runnable task) {
        shutdownHooks.add(task);
    }

    private static final Filter<HttpServletContext>[] processors = new Filter[]{
            new TrustedIPChecker(),
            new JspInterceptor(),
            new I18nFilter(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
            new Manual(),
            new ResetPassword(),
            new LastDecision()
    };

    private static final class LastDecision implements Filter<HttpServletContext> {

        @Override
        public boolean doFilter(HttpServletContext context) throws Exception {
            context.chain.doFilter(context.req, context.resp); // 这样可以进入 servlet 资源
            return false;
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            COMPONENT_SEQUENCE.exec();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        COMPONENT_SEQUENCE.undo();
        shutdownHooks.forEach(Runnable::run);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletContext context = new HttpServletContext(httpServletRequest, httpServletResponse, chain);
        try {
            FilterPattern.doFilter(context, processors);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private static class InitConsoleMenu implements Process {

        @Override
        public void exec() {
//      todo      ConsoleContext master = Main.getInternalService(ConsoleContextFinder.class).find(Constants.QINGZHOU_MASTER_APP_NAME);
            ConsoleContext master = ConsoleUtil.getAppContext(null).getConsoleContext();
            master.setMenuInfo("Favorites", new String[]{"我的收藏", "en:Favorites"}, "star", 0);
            master.setMenuInfo("Service", new String[]{"服务管理", "en:Service"}, "server", 1);
            master.setMenuInfo("Product", new String[]{"产品管理", "en:Product"}, "book", 2);
            master.setMenuInfo("Security", new String[]{"安全管理", "en:Security"}, "shield", 3);
            master.setMenuInfo("System", new String[]{"系统管理", "en:System"}, "cog", 4);
            master.setMenuInfo("Support", new String[]{"扩展支持", "en:Support"}, "rocket", 5);

//      todo      ConsoleContext defaultApp = Main.getInternalService(ConsoleContextFinder.class).find(Constants.QINGZHOU_DEFAULT_APP_NAME);
            ConsoleContext defaultApp = ConsoleUtil.getAppContext(null).getConsoleContext();
            master.setMenuInfo("Favorites", new String[]{"我的收藏", "en:Favorites"}, "star", 0);
            defaultApp.setMenuInfo("Monitor", new String[]{"监视管理", "en:Monitor"}, "line-chart", 1);
            defaultApp.setMenuInfo("Security", new String[]{"安全配置", "en:Security"}, "shield", 2);
            defaultApp.setMenuInfo("Basic", new String[]{"基础配置", "en:Basic"}, "stack", 3);
        }
    }
}
