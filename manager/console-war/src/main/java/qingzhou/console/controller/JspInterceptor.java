package qingzhou.console.controller;

import javax.servlet.http.HttpServletResponse;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

public class JspInterceptor implements Filter<SystemControllerContext> {
    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        String checkPath = RESTController.getReqUri(context.req);
        if (Utils.notBlank(checkPath) && (
                checkPath.trim().endsWith(".jsp") || checkPath.trim().endsWith(".jspx")
        )) {
            HttpServletResponse resp = context.resp;
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}
