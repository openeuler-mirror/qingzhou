package qingzhou.console.view.type;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.DownloadData;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

public class DownloadView implements View {
    public static final String FLAG = "download";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = (ResponseImpl) request.getResponse();
        DownloadData downloadData = (DownloadData) response.getInternalData();
        String downloadName = downloadData.downloadName;
        if (Utils.isBlank(downloadName)) {
            downloadName = request.getId();
        }
        if (Utils.isBlank(downloadName)) {
            downloadName = request.getModel() + "-" + System.currentTimeMillis();
        }
        HttpServletResponse servletResponse = restContext.resp;
        String encodedFileName = URLEncoder.encode(downloadName, "UTF-8");
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + encodedFileName);

        byte[] content = downloadData.block;
        Map<String, String> result = response.getParameters();
        while (true) {
            if (content == null || content.length == 0) break;

            ServletOutputStream outputStream = servletResponse.getOutputStream();
            outputStream.write(content);
            outputStream.flush();

            // 判断是否需要续传
            String s = result.get(DownloadData.DOWNLOAD_OFFSET);
            if (s == null) break;
            long offset = Long.parseLong(s);
            if (offset <= 0) break;

            // 要续传
            RequestImpl req = new RequestImpl(request);
            req.getParameters().put(DownloadData.DOWNLOAD_SERIAL_KEY, result.get(DownloadData.DOWNLOAD_SERIAL_KEY));
            req.getParameters().put(DownloadData.DOWNLOAD_OFFSET, String.valueOf(offset));
            ResponseImpl res = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeSingle(req);
            if (res.isSuccess()) {
                content = (byte[]) res.getInternalData();
                result = res.getParameters();
            } else {
                response.setSuccess(false);
                response.setMsg(res.getMsg());
                break;
            }
        }
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }
}
