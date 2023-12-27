package qingzhou.console.view.impl;

import qingzhou.console.controller.RestContext;
import qingzhou.api.console.data.Datas;
import qingzhou.api.console.data.Response;
import qingzhou.console.DatasImpl;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class JsonView implements View {
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
        return "application/json;charset=UTF-8";
    }

    public static String buildErrorResponse(String errorMsg) {
        StringBuilder json = new StringBuilder("{");
        addBasic(json, false, errorMsg);
        return json.toString();
    }

    private static String convertToJson(Response response) {
        StringBuilder json = new StringBuilder("{");

        addBasic(json, response.isSuccess(), response.getMsg());
        json.append(",");
        addModelData(json, "model", response.modelData());
        json.append(",");
        addModelData(json, "monitor", response.monitorData());
        json.append(",");
        addModelData(json, "error", response.errorData());
        json.append(",");
        addModelData(json, "attachment", response.attachmentData());

        json.append("}");
        return json.toString();
    }

    private static void addBasic(StringBuilder json, boolean success, String message) {
        addKV(json, "success", String.valueOf(success));
        json.append(",");
        addKV(json, "message", message);
    }

    private static void addModelData(StringBuilder json, String k, Datas di) {
        DatasImpl dataInfo = (DatasImpl) di;
        json.append("\"").append(k).append("\"");
        json.append(":");
        json.append("[");

        List<Map<String, String>> dataList = dataInfo.getDataList();
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

        if (hasPageInfo(dataInfo)) {
            json.append(",");
            json.append("\"").append(k).append(".page").append("\"");
            json.append(":{\"totalSize\":\"").append(dataInfo.getTotalSize())
                    .append("\",\"pageSize\":\"").append(dataInfo.getPageSize())
                    .append("\",\"pageNum\":\"").append(dataInfo.getPageNum())
                    .append("\"}");
        }
    }

    private static boolean hasPageInfo(Datas datas) {
        return datas.getTotalSize() >= 0 && datas.getPageSize() >= 0 && datas.getPageNum() >= 0;
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