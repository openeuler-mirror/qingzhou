package qingzhou.console.controller.system;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StreamUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class Manual implements Filter<HttpServletContext> {
    private byte[] pdfCache;

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        String checkPath = PageBackendService.retrieveServletPathAndPathInfo(context.req);
        if (!checkPath.equals("/" + ConsoleConstants.MANUAL_PDF)) {
            return true;
        }

        HttpServletResponse response = context.resp;
        response.setContentType("application/pdf; charset=UTF-8");
        ServletOutputStream out = response.getOutputStream();
        if (pdfCache == null) {
            File file = null;
            File docDir = new File(ConsoleWarHelper.getLib(), "docs");
            String[] docs = docDir.list();
            if (docs != null) {
                for (String doc : docs) {
                    if (doc.endsWith("-User-Guide.pdf")) { // 适配 doc 文件名修改为 7.0.8.X
                        file = FileUtil.newFile(docDir, doc); // 中文，可能极少系统无法识别吗？
                        break;
                    }
                }
            }

            if (file != null && file.exists()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (FileInputStream fis = new FileInputStream(file)) {
                    StreamUtil.copyStream(fis, bos);
                }
                pdfCache = bos.toByteArray();
            }
        }
        if (pdfCache != null) {
            out.write(pdfCache);
        } else {
            out.write("No manual file found !".getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
        return false;
    }
}
