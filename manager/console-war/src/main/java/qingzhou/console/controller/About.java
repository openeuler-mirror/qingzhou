package qingzhou.console.controller;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.util.FileUtil;
import qingzhou.engine.util.pattern.Filter;

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
        String checkPath = RESTController.retrieveServletPathAndPathInfo(context.req);
        if (!checkPath.equals(ABOUT_URI)) {
            return true;
        }

        HttpServletResponse response = context.resp;
        response.setContentType("text/plain; charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();

        if (byteCache == null) {
            String readmeDir = "static/readme/";
            String readmePath = context.req.getServletContext().getRealPath("/" + readmeDir + "/README.md");
            List<String> fileLines = FileUtil.fileToLines(new File(readmePath));
            StringBuilder result = new StringBuilder();
            String begin = "![";
            String end = ")";
            fileLines.forEach(line -> {
                if (line.trim().startsWith(begin) && line.trim().endsWith(end)) {
                    int index = line.indexOf("](") + 2;
                    String imgName = line.substring(index, line.lastIndexOf(end));
                    imgName = imgName.replace("doc/readme/", readmeDir);
                    result.append("![](")
                            .append(context.req.getContextPath())
                            .append("/").append(imgName)
                            .append(")");
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
