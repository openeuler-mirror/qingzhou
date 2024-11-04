package qingzhou.console.view.type;

import qingzhou.api.MsgLevel;
import qingzhou.api.Request;
import qingzhou.api.type.Monitor;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.View;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;
import qingzhou.registry.ModelInfo;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

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
        RequestImpl request = restContext.request;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ResponseImpl response = (ResponseImpl) request.getResponse();
        String writeJson;

        Serializable customizedDataObject = response.getCustomizedDataObject();
        if (customizedDataObject != null) {
            if (customizedDataObject instanceof String) {
                writeJson = (String) customizedDataObject;
            } else {
                Json json = SystemController.getService(Json.class);
                writeJson = json.toJson(customizedDataObject);
            }
        } else {
            switch (request.getAction()) {
                case Monitor.ACTION_MONITOR:
                    if (!response.getDataMap().isEmpty()) {
                        String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
                        Map<String, String> orderedData = new LinkedHashMap<>();

                        for (String fieldName : monitorFieldNames) {
                            orderedData.put(fieldName, response.getDataMap().get(fieldName));
                        }
                        response.getDataMap().clear();
                        response.getDataMap().putAll(orderedData);
                    }
                    break;
                case qingzhou.api.type.List.ACTION_LIST:
                    if (modelInfo.isHideId()) {
                        int idIndex = modelInfo.getIdIndex();
                        List<String[]> dataList = response.getDataList();
                        for (int i = 0; i < dataList.size(); i++) {
                            List<String> temp = new ArrayList<>(Arrays.asList(dataList.get(i)));
                            temp.remove(idIndex);
                            dataList.set(i, temp.toArray(new String[0]));
                        }
                    }
                    break;
            }
            writeJson = buildJsonResult(request);
        }

        PrintWriter writer = restContext.resp.getWriter();
        writer.write(writeJson);
        writer.flush();
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    private static String buildJsonResult(Request request) {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        MsgLevel msgLevel = response.getMsgLevel() != null ? response.getMsgLevel() : MsgLevel.ERROR;
        StringBuilder json = buildJsonHead(response.isSuccess(), response.getMsg(), msgLevel.flag);
        String dataKey = "data";

        Map<String, String> errorInfo = response.getErrorInfo();
        if (!errorInfo.isEmpty()) {
            return endWithMap(json, errorInfo, dataKey);
        }

        Map<String, String> dataMap = response.getDataMap();
        if (!dataMap.isEmpty()) {
            return endWithMap(json, dataMap, dataKey);
        }

        List<String[]> dataList = response.getDataList();
        if (!dataList.isEmpty()) {
            json.append(",");
            json.append("\"").append(dataKey).append("\"");
            json.append(":");
            json.append("[");
            for (int i = 0; i < dataList.size(); i++) {
                if (i > 0) json.append(",");
                addArray(json, dataList.get(i));
            }
            json.append("]");
            endJson(json);
            return json.toString();
        }

        endJson(json);
        return json.toString();
    }

    private static String endWithMap(StringBuilder json, Map<String, String> map, String key) {
        json.append(",");
        json.append("\"").append(key).append("\"");
        json.append(":");
        addMap(json, map);
        endJson(json);
        return json.toString();
    }

    private static StringBuilder buildJsonHead(boolean success, String msg, String msgLevel) {
        StringBuilder json = new StringBuilder("{");
        addKV(json, "success", String.valueOf(success));
        json.append(",");
        addKV(json, "msg", msg);
        json.append(",");
        addKV(json, "msg_level", msgLevel);
        return json;
    }

    private static void endJson(StringBuilder json) {
        json.append("}");
    }

    private static void addArray(StringBuilder json, String[] data) {
        json.append("[");
        boolean notFirst = false;
        for (String entry : data) {
            if (notFirst) json.append(",");
            notFirst = true;
            json.append("\"").append(entry).append("\"");
        }
        json.append("]");
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