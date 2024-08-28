package qingzhou.console.view.type;

import qingzhou.api.Response;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.RequestImpl;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileView implements View {
    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        Response response = request.getResponse();
        String fileName = (request.getId() == null || "".equals(request.getId())) ? (request.getModel() + "-" + System.currentTimeMillis()) : request.getId();
        HttpServletResponse servletResponse = restContext.servletResponse;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");

        List<Map<String, String>> dataList = response.getDataList();
        if (dataList.isEmpty()) {
            response.setSuccess(false);
            return;
        }

        Map<String, String> result = dataList.get(0);
        String key = result.get(ConsoleConstants.DOWNLOAD_KEY);
        long offset = Long.parseLong(result.get(ConsoleConstants.DOWNLOAD_OFFSET));
        while (true) {
            byte[] content = SystemController.getService(CryptoService.class).getHexCoder().hexToBytes(result.get(ConsoleConstants.DOWNLOAD_BLOCK));

            ServletOutputStream outputStream = servletResponse.getOutputStream();
            outputStream.write(content);
            outputStream.flush();
            if (offset < 0) {
                break;
            }

            RequestImpl req = request.clone();
            Map<String, String> data = new HashMap<>();
            data.put(ConsoleConstants.DOWNLOAD_KEY, key);
            data.put(ConsoleConstants.DOWNLOAD_OFFSET, String.valueOf(offset));
            req.setParameters(data);
            ActionInvoker.getInstance().invokeAction(req); // 续传
            Response res = req.getResponse();
            if (res.isSuccess()) {
                result = res.getDataList().get(0);
                offset = Long.parseLong(result.get(ConsoleConstants.DOWNLOAD_OFFSET));
                key = result.get(ConsoleConstants.DOWNLOAD_KEY);
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
