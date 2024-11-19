package qingzhou.console.view.type;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.LinkedHashMap;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.MsgLevel;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.json.Json;

public class JsonView implements View {
    public static final String FLAG = DeployerConstants.JSON_VIEW_FLAG;

    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    public static String responseErrorJson(HttpServletResponse response, String msg) throws IOException {
        LinkedHashMap<String, Object> result = buildJsonHead(false, msg, MsgLevel.ERROR);
        Json json = SystemController.getService(Json.class);
        String writeJson = json.toJson(result);

        response.setContentType(CONTENT_TYPE);
        response.getWriter().print(writeJson);

        return writeJson;
    }

    @Override
    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = (ResponseImpl) request.getResponse();

        Serializable responseData = response.getAppData();
        if (responseData == null) {
            LinkedHashMap<String, Object> result = buildJsonHead(response.isSuccess(), response.getMsg(), response.getMsgLevel());
            result.put("data", response.getInternalData());
            responseData = result;
        }

        String writeJson;
        if (responseData instanceof String) {
            writeJson = (String) responseData;
        } else {
            Json json = SystemController.getService(Json.class);
            writeJson = json.toJson(responseData);
        }

        PrintWriter writer = restContext.resp.getWriter();
        writer.write(writeJson);
        writer.flush();
    }

    private static LinkedHashMap<String, Object> buildJsonHead(boolean success, String msg, MsgLevel msgLevel) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", String.valueOf(success));
        result.put("msg", msg);
        if (msgLevel == null) {
            msgLevel = success ? MsgLevel.INFO : MsgLevel.ERROR;
        }
        result.put("msg_level", msgLevel.flag);
        return result;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }
}