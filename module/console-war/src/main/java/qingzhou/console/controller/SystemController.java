package qingzhou.console.controller;

import qingzhou.console.ConsoleWarHelper;
import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.framework.util.pattern.Filter;
import qingzhou.framework.util.pattern.FilterPattern;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    private final Filter<HttpServletContext>[] processors = new Filter[]{
            new TrustedIPChecker(),
            new JspInterceptor(),
            new SetI18n(),
            new About(),
            new NodeRegister(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
            new ResetPassword(),
            new AccessControl(),
            new SearchFilter(),
            new LastDecision()
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletContext context = new HttpServletContext(httpServletRequest, httpServletResponse, chain);
        try {
            FilterPattern.doFilter(context, processors);
        } catch (Throwable e) {
            ConsoleWarHelper.getLogger().error(e.getMessage(), e);
        }
    }

    private static final class LastDecision implements Filter<HttpServletContext> {

        @Override
        public boolean doFilter(HttpServletContext context) throws Exception {
            context.chain.doFilter(context.req, context.resp); // 这样可以进入 servlet 资源
            return false;
        }
    }
}
