package qingzhou.console.security;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.controller.HttpServletContext;
import qingzhou.framework.pattern.Filter;
import qingzhou.console.util.StringUtil;

import javax.servlet.http.HttpServletResponse;

public class JspInterceptor implements Filter<HttpServletContext> {
    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = ConsoleUtil.retrieveServletPathAndPathInfo(context.req);
        if (StringUtil.notBlank(checkPath) && (
                checkPath.trim().endsWith(".jsp") || checkPath.trim().endsWith(".jspx")
        )) {
            HttpServletResponse resp = context.resp;
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
