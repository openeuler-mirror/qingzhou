package qingzhou.console.view.type;

import qingzhou.api.DownloadModel;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.ActionInvoker;
import qingzhou.console.RestContext;
import qingzhou.console.util.HexUtil;
import qingzhou.console.view.View;
import qingzhou.framework.app.RequestImpl;
import qingzhou.framework.app.ResponseImpl;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileView implements View {
    @Override
    public void render(RestContext restContext) throws Exception {
        Request request = restContext.request;
        qingzhou.framework.app.ResponseImpl response = (qingzhou.framework.app.ResponseImpl) restContext.response;
        String fileName = (request.getId() == null || "".equals(request.getId())) ? (request.getModelName() + "-" + System.currentTimeMillis()) : request.getId();
        HttpServletResponse servletResponse = restContext.servletResponse;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");

        List<Map<String, String>> dataList = response.getDataList();
        if (dataList.isEmpty()) {
            response.setSuccess(false);
            return;
        }

        Map<String, String> result = dataList.get(0);
        String key = result.get(DownloadModel.DOWNLOAD_KEY);
        long offset = Long.parseLong(result.get(DownloadModel.DOWNLOAD_OFFSET));
        while (true) {
            byte[] content = HexUtil.hexToBytes(result.get(DownloadModel.DOWNLOAD_BLOCK));

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
            ResponseImpl res = ActionInvoker.getInstance().invokeAction(req); // 续传
            if (res.isSuccess()) {
                result = res.getDataList().get(0);
                offset = Long.parseLong(result.get(DownloadModel.DOWNLOAD_OFFSET));
                key = result.get(DownloadModel.DOWNLOAD_KEY);
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
