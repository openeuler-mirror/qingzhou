package qingzhou.console.controller;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class Theme implements Filter<SystemControllerContext> {
    public static final String URI_THEME = "/theme";
    public static final String KEY_THEME_MODE = "theme_mode";

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        String checkPath = RESTController.getReqUri(context.req);
        if (!checkPath.startsWith(URI_THEME)) return true;

        HttpServletResponse response = context.resp;
        response.setContentType("text/plain; charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();
        boolean isDarkTheme = checkPath.startsWith(URI_THEME + "/dark");
        context.req.getSession(false).setAttribute(KEY_THEME_MODE, isDarkTheme ? "dark" : "");
        out.write((isDarkTheme ? "dark" : "").getBytes());
        out.flush();
        return false;
    }
}
