package qingzhou.console.controller.system;

import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.pattern.FilterPattern;
import qingzhou.framework.pattern.Process;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    private static final List<Runnable> shutdownHooks = new ArrayList<>();

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
//      todo      ConsoleContext defaultApp = Main.getInternalService(ConsoleContextFinder.class).find(Constants.QINGZHOU_DEFAULT_APP_NAME);
            /*ConsoleContext defaultApp = ConsoleUtil.getAppContext(null).getConsoleContext();
            defaultApp.setMenuInfo("Monitor", new String[]{"监视管理", "en:Monitor"}, "line-chart", 1);
            defaultApp.setMenuInfo("Security", new String[]{"安全配置", "en:Security"}, "shield", 2);
            defaultApp.setMenuInfo("Basic", new String[]{"基础配置", "en:Basic"}, "stack", 3);*/
        }
    }
}
