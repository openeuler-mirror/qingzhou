package qingzhou.console.controller;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.StringUtil;

import javax.servlet.http.HttpServletResponse;

public class JspInterceptor implements Filter<HttpServletContext> {
    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = RESTController.retrieveServletPathAndPathInfo(context.req);
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
