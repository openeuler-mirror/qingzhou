package qingzhou.console.view.type;

import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class StreamView implements View {
    public static final String FLAG = "stream";

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = (ResponseImpl) request.getResponse();
        String downloadName = response.getDownloadName();
        if (Utils.isBlank(downloadName)) {
            downloadName = request.getId();
        }
        if (Utils.isBlank(downloadName)) {
            downloadName = request.getModel() + "-" + System.currentTimeMillis();
        }
        HttpServletResponse servletResponse = restContext.resp;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + downloadName);

        byte[] content = response.getBodyBytes();
        Map<String, String> result = response.getParameters();
        while (true) {
            if (content == null || content.length == 0) break;

            ServletOutputStream outputStream = servletResponse.getOutputStream();
            outputStream.write(content);
            outputStream.flush();

            // 判断是否需要续传
            String s = result.get(DeployerConstants.DOWNLOAD_OFFSET);
            if (s == null) break;
            long offset = Long.parseLong(s);
            if (offset <= 0) break;

            // 要续传
            RequestImpl req = new RequestImpl(request);
            req.setParameter(DeployerConstants.DOWNLOAD_SERIAL_KEY, result.get(DeployerConstants.DOWNLOAD_SERIAL_KEY));
            req.setParameter(DeployerConstants.DOWNLOAD_OFFSET, String.valueOf(offset));
            ResponseImpl res = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeSingle(req);
            if (res.isSuccess()) {
                content = res.getBodyBytes();
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
