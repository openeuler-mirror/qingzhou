package qingzhou.console.view.type;

import qingzhou.api.MsgLevel;
import qingzhou.api.Request;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class JsonView implements View {
    public static final String FLAG = DeployerConstants.JSON_VIEW_FLAG;

    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    public static String responseErrorJson(HttpServletResponse response, String msg) throws IOException {
        StringBuilder json = buildJsonHead(false, msg, MsgLevel.ERROR.name());
        endJson(json);

        response.setContentType(CONTENT_TYPE);
        response.getWriter().print(json);

        return json.toString();
    }

    @Override
    public void render(RestContext restContext) throws Exception {
        String json = buildJsonResult(restContext.request);
        PrintWriter writer = restContext.resp.getWriter();
        writer.write(json);
        writer.flush();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    private static String buildJsonResult(Request request) {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        MsgLevel msgLevel = response.getMsgLevel() != null ? response.getMsgLevel() : MsgLevel.ERROR;
        StringBuilder json = buildJsonHead(response.isSuccess(), response.getMsg(), msgLevel.name());

        Map<String, String> errorInfo = response.getErrorInfo();
        if (!errorInfo.isEmpty()) {
            json.append(",");
            json.append("\"error\"");
            json.append(":");
            addMap(json, errorInfo);
            endJson(json);
            return json.toString();
        }

        Map<String, String> dataMap = response.getDataMap();
        if (!dataMap.isEmpty()) {
            json.append(",");
            json.append("\"data\"");
            json.append(":");
            addMap(json, dataMap);
            endJson(json);
            return json.toString();
        }

        List<Map<String, String>> dataList = response.getDataList();
        if (!dataList.isEmpty()) {
            json.append(",");
            json.append("\"list\"");
            json.append(":");
            json.append("[");
            for (int i = 0; i < dataList.size(); i++) {
                if (i > 0) json.append(",");
                addMap(json, dataList.get(i));
            }
            json.append("]");

            json.append(",");
            json.append("\"list-page\"");
            json.append(":{\"totalSize\":\"").append(response.getTotalSize())
                    .append("\",\"pageSize\":\"").append(response.getPageSize())
                    .append("\",\"pageNum\":\"").append(response.getPageNum())
                    .append("\"}");
            endJson(json);
            return json.toString();
        }

        return "";
    }


    private static StringBuilder buildJsonHead(boolean success, String message, String msg_level) {
        StringBuilder json = new StringBuilder("{");
        addKV(json, "success", String.valueOf(success));
        json.append(",");
        addKV(json, "msg_level", msg_level);
        json.append(",");
        addKV(json, "message", message);
        return json;
    }

    private static void endJson(StringBuilder json) {
        json.append("}");
    }

    private static void addMap(StringBuilder json, Map<String, String> data) {
        json.append("{");
        boolean notFirst = false;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() == null) continue;
            if (notFirst) json.append(",");
            notFirst = true;

            addKV(json, entry.getKey(), entry.getValue());
        }
        json.append("}");
    }

    private static void addKV(StringBuilder json, String k, String v) {
        json.append("\"").append(k).append("\"");
        json.append(":");
        json.append("\"").append(escapeChar(v)).append("\"");
    }

    private static String escapeChar(String json) {
        if (Utils.isBlank(json)) return "";
        return json.replace("\t", " ") // for html ?
                .replace("\\", "\\\\") // for windows 路径 & ITAIT-3687
                .replace("\"", "\\\"")
                .replace("\r", "\\r") // 兼容 本机是window  json 是 liunx
                .replace("\n", "\\n") // 兼容 本机是window  json 是 liunx
                ;
    }
}