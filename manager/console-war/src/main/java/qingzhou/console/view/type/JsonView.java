package qingzhou.console.view.type;

import qingzhou.api.MsgLevel;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class JsonView implements View {
    public static final String FLAG = DeployerConstants.JSON_VIEW_FLAG;

    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    @Override
    public void render(RestContext restContext) throws Exception {
        String json = convertToJson(restContext.request);
        PrintWriter writer = restContext.resp.getWriter();
        writer.write(json);
        writer.flush();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    public static String responseErrorJson(HttpServletResponse response, String msg) throws IOException {
        StringBuilder json = new StringBuilder("{");
        addStatus(json, false, msg, MsgLevel.error.name());
        json.append("}");

        response.setContentType(CONTENT_TYPE);
        response.getWriter().print(json);

        return json.toString();
    }

    private static String convertToJson(Request request) {
        StringBuilder json = new StringBuilder("{");

        Response response = request.getResponse();
        MsgLevel msgLevel = response.getMsgType() != null ? response.getMsgType() : MsgLevel.error;
        addStatus(json, response.isSuccess(), response.getMsg(), msgLevel.name());
        json.append(",");
        addData(json, request);

        json.append("}");
        return json.toString();
    }

    private static void addStatus(StringBuilder json, boolean success, String message, String msg_level) {
        addKV(json, "success", String.valueOf(success));
        json.append(",");
        addKV(json, "msg_level", msg_level);
        json.append(",");
        addKV(json, "message", message);
    }

    private static void addData(StringBuilder json, Request request) {
        json.append("\"").append(DeployerConstants.JSON_DATA).append("\"");
        json.append(":");
        json.append("[");

        Response response = request.getResponse();
        List<Map<String, String>> dataList = response.getDataList();
        for (int i = 0; i < dataList.size(); i++) {
            if (i > 0) {
                json.append(",");
            }

            json.append("{");
            Map<String, String> data = dataList.get(i);
            boolean notFirst = false;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (null == entry.getValue()) {
                    continue;
                }
                if (notFirst) {
                    json.append(",");
                }
                notFirst = true;

                addKV(json, entry.getKey(), entry.getValue());
            }
            json.append("}");
        }

        json.append("]");

        if (request.getAction().equals(qingzhou.api.type.List.ACTION_LIST)) {
            json.append(",");
            json.append("\"").append("page-info").append("\"");
            json.append(":{\"totalSize\":\"").append(response.getTotalSize())
                    .append("\",\"pageSize\":\"").append(response.getPageSize())
                    .append("\",\"pageNum\":\"").append(response.getPageNum())
                    .append("\"}");
        }
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