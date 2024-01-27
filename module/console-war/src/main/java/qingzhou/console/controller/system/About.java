package qingzhou.console.controller.system;

import qingzhou.console.page.PageBackendService;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.FileUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class About implements Filter<HttpServletContext> {
    public static final String ABOUT_URI = "/about";
    private byte[] byteCache;

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = PageBackendService.retrieveServletPathAndPathInfo(context.req);
        if (!checkPath.equals(ABOUT_URI)) {
            return true;
        }

        HttpServletResponse response = context.resp;
        response.setContentType("text/plain; charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();

        if (byteCache == null) {
            String pathPrefix = "/static/readme/";
            String readmePath = context.req.getServletContext().getRealPath(pathPrefix + "/README.md");
            List<String> fileLines = FileUtil.fileToLines(new File(readmePath));
            StringBuilder result = new StringBuilder();
            fileLines.forEach(line -> {
                if (line.contains("architecture.jpg")) {
                    result.append("![architecture](")
                            .append(context.req.getContextPath())
                            .append(pathPrefix)
                            .append("/architecture.jpg)");
                } else {
                    result.append(line);
                }
                result.append("\n");
            });

            byteCache = result.toString().getBytes(StandardCharsets.UTF_8);
        }

        out.write(byteCache);
        out.flush();
        return false;
    }
}
