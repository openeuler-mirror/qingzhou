package qingzhou.console.view.impl;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.framework.api.DownloadModel;
import qingzhou.framework.api.Request;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.console.ResponseImpl;
import qingzhou.console.DownLoadUtil;
import qingzhou.framework.util.TimeUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class FileView implements View {
    @Override
    public void render(RestContext restContext) throws Exception {
        Request request = restContext.request;
        ResponseImpl response = (ResponseImpl) restContext.response;
        String fileName = (request.getId() == null || "".equals(request.getId())) ? (request.getModelName() + "-" + TimeUtil.getCurrentTime()) : request.getId();
        HttpServletResponse servletResponse = restContext.servletResponse;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");

        Map<String, Object> result = null;// todo: response.downloadData()
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
            HashMap<String, String> data = new HashMap<>();
            data.put(DownloadModel.DOWNLOAD_KEY, key);
            data.put(DownloadModel.DOWNLOAD_OFFSET, String.valueOf(offset));
            req.setParameters(data);
            ResponseImpl res = (ResponseImpl) RESTController.invokeAction.invoke(req);
            if (res.isSuccess()) {
                result = null; // todo: res.downloadData();
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
