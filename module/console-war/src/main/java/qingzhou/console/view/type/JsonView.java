package qingzhou.console.view.type;

import qingzhou.api.Response;
import qingzhou.console.RestContext;
import qingzhou.console.view.View;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class JsonView implements View {
    public static final String CONTENT_TYPE = "application/json;charset=UTF-8";

    @Override
    public void render(RestContext restContext) throws Exception {
        Response response = restContext.response;
        String json = convertToJson(response);
        PrintWriter writer = restContext.servletResponse.getWriter();
        writer.write(json);
        writer.flush();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    public static String responseErrorJson(HttpServletResponse response, String msg) throws IOException {
        StringBuilder json = new StringBuilder("{");
        addStatus(json, false, msg);
        json.append("}");

        response.setContentType(CONTENT_TYPE);
        response.getWriter().print(json);

        return json.toString();
    }

    private static String convertToJson(Response response) {
        StringBuilder json = new StringBuilder("{");

        addStatus(json, response.isSuccess(), response.getMsg());
        json.append(",");
        addData(json, response);

        json.append("}");
        return json.toString();
    }

    private static void addStatus(StringBuilder json, boolean success, String message) {
        addKV(json, "success", String.valueOf(success));
        json.append(",");
        addKV(json, "message", message);
    }

    private static void addData(StringBuilder json, Response response) {
        json.append("\"").append("data").append("\"");
        json.append(":");
        json.append("[");

        List<Map<String, String>> dataList = response.getDataList();
        for (int i = 0; i < dataList.size(); i++) {
            if (i > 0) {
                json.append(",");
            }

            json.append("{");
            Map<String, String> data = dataList.get(i);
            boolean notFirst = false;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (notFirst) {
                    json.append(",");
                }
                notFirst = true;

                addKV(json, entry.getKey(), entry.getValue());
            }
            json.append("}");
        }

        json.append("]");

        json.append(",");
        json.append("\"").append(".page-info").append("\"");
        json.append(":{\"totalSize\":\"").append(response.getTotalSize())
                .append("\",\"pageSize\":\"").append(response.getPageSize())
                .append("\",\"pageNum\":\"").append(response.getPageNum())
                .append("\"}");
    }

    private static void addKV(StringBuilder json, String k, String v) {
        json.append("\"").append(k).append("\"");
        json.append(":");
        json.append("\"").append(escapeChar(v)).append("\"");
    }

    private static String escapeChar(String json) {
        return json.replace("\t", " ") // for html ?
                .replace("\\", "\\\\") // for windows 路径 & ITAIT-3687
                .replace("\"", "\\\"")
                .replace("\r", "\\r") // 兼容 本机是window  json 是 liunx
                .replace("\n", "\\n") // 兼容 本机是window  json 是 liunx
                ;
    }
}