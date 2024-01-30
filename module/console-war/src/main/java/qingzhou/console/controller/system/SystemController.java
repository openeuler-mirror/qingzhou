package qingzhou.console.controller.system;

import qingzhou.console.login.LoginFreeFilter;
import qingzhou.console.login.LoginManager;
import qingzhou.console.login.ResetPassword;
import qingzhou.console.login.vercode.VerCode;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.pattern.FilterPattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SystemController implements ServletContextListener, javax.servlet.Filter {
    private static final Filter<HttpServletContext>[] processors = new Filter[]{
            new TrustedIPChecker(),
            new JspInterceptor(),
            new I18nFilter(),
            new About(),
            new NodeRegister(),
            new VerCode(),
            new LoginFreeFilter(),
            new LoginManager(),
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
}
