package qingzhou.console.controller.system;

import java.io.BufferedReader;
import qingzhou.console.ConsoleConstants;
import qingzhou.framework.pattern.Filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import qingzhou.console.page.PageBackendService;

public class About implements Filter<HttpServletContext> {

    private byte[] byteCache;
    private final String README_PATH = "static" + File.separator + "help" + File.separator + "README.md";
    private final String ARCHITECTURE_LINE = "![软件架构](static/images/architecture.jpg)";

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = PageBackendService.retrieveServletPathAndPathInfo(context.req);
        if (!checkPath.equals(ConsoleConstants.ABOUT_URI)) {
            return true;
        }

        HttpServletResponse response = context.resp;
        response.setContentType("text/plain; charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();

        if (byteCache == null) {
            String classPath = Thread.currentThread().getContextClassLoader().getResource("/").getPath();
            File file = new File(new File(classPath).getParentFile().getParentFile(), README_PATH);
            StringBuilder builder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("architecture.jpg")) {
                        builder.append(ARCHITECTURE_LINE);
                    } else {
                        builder.append(line);
                    }
                    builder.append("\n");
                }
                br.close();
            }
            byteCache = builder.toString().getBytes(StandardCharsets.UTF_8);
        }
        if (byteCache != null) {
            out.write(byteCache);
        } else {
            out.write("No manual file found !".getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
        return false;
    }
}
