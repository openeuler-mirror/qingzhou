package qingzhou.console.view.type;

import qingzhou.api.Response;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class FileView implements View {
    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        Response response = request.getResponse();
        String fileName = (request.getId() == null || "".equals(request.getId())) ? (request.getModel() + "-" + System.currentTimeMillis()) : request.getId();
        HttpServletResponse servletResponse = restContext.resp;
        servletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName + ".zip");

        List<Map<String, String>> dataList = response.getDataList();
        if (dataList.isEmpty()) {
            response.setSuccess(false);
            return;
        }

        Map<String, String> result = dataList.get(0);
        while (true) {
            byte[] content = SystemController.getService(CryptoService.class).getBase64Coder().decode(result.get(DeployerConstants.DOWNLOAD_BLOCK));

            ServletOutputStream outputStream = servletResponse.getOutputStream();
            outputStream.write(content);
            outputStream.flush();

            long offset = Long.parseLong(result.get(DeployerConstants.DOWNLOAD_OFFSET));
            if (offset < 0) break;

            RequestImpl req = new RequestImpl(request);
            req.setNonModelParameter(DeployerConstants.DOWNLOAD_KEY, result.get(DeployerConstants.DOWNLOAD_KEY));
            req.setNonModelParameter(DeployerConstants.DOWNLOAD_OFFSET, String.valueOf(offset));
            Response res = SystemController.getService(ActionInvoker.class).invokeSingle(req); // 续传
            if (res.isSuccess()) {
                result = res.getDataList().get(0);
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
