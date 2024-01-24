package qingzhou.console.controller.system;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.view.impl.HtmlView;
import qingzhou.console.ConsoleConstants;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StreamUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class About implements Filter<HttpServletContext> {

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = ConsoleUtil.retrieveServletPathAndPathInfo(context.req);
        if (!checkPath.equals("/about")) {
            return true;
        }
        context.req.getRequestDispatcher(HtmlView.htmlPageBase + "view/about.jsp").forward(context.req, context.resp);
        return false;
    }
}
