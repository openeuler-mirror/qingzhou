package qingzhou.console.view.impl;

import qingzhou.console.controller.InvokeAction;
import qingzhou.console.controller.RestContext;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.DownloadModel;
import qingzhou.console.RequestImpl;
import qingzhou.console.util.DownLoadUtil;
import qingzhou.console.util.TimeUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class FileView implements View {
    @Override
    public void render(RestContext restContext) throws Exception {
        Request request = restContext.request;
        Response response = restContext.response;
        String fileName = (request.getId() == null || "".equals(request.getId())) ? (request.getModelName() + "-" + TimeUtil.getCurrentTime()) : request.getId();
        HttpServletResponse servletResponse = restContext.servletResponse;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");

        Map<String, Object> result = response.downloadData();
        if (result.isEmpty()) {
            return;
        }

        String key = (String) result.get(DownloadModel.DOWNLOAD_KEY);
        long offset = (long) result.get(DownloadModel.DOWNLOAD_OFFSET);
        while (true) {
            byte[] content = (byte[]) result.get(DownLoadUtil.DOWNLOAD_BLOCK);
            if (content == null) {
                break;
            }

            ServletOutputStream outputStream = servletResponse.getOutputStream();
            outputStream.write(content);
            outputStream.flush();
            if (offset < 0) {
                break;
            }

            RequestImpl req = ((RequestImpl) request).clone();
            req.setParameterNames(new String[]{DownloadModel.DOWNLOAD_KEY, DownloadModel.DOWNLOAD_OFFSET});
            req.setParameterValues(new String[]{key, String.valueOf(offset)});
            Response res = new InvokeAction().invoke(req);
            if (res.isSuccess()) {
                result = res.downloadData();
                offset = (long) result.get(DownloadModel.DOWNLOAD_OFFSET);  // 续传
                key = (String) result.get(DownloadModel.DOWNLOAD_KEY);      // 性能优化
            } else {
                response.setSuccess(false);
                response.setMsg(res.getMsg());
                return;
            }
        }
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }
}
