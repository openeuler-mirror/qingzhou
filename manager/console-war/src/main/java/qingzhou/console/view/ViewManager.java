package qingzhou.console.view;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.type.Show;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.console.view.type.DownloadView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

public class ViewManager {
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(HtmlView.FLAG, new HtmlView());
        views.put(JsonView.FLAG, new JsonView());
        views.put(DownloadView.FLAG, new DownloadView());
    }

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = (ResponseImpl) request.getResponse();
        HttpServletRequest servletRequest = restContext.req;
        HttpServletResponse servletResponse = restContext.resp;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());

        if (servletResponse.isCommitted()) return;

        servletResponse.setContentType(response.getContentType());
        response.getHeaderNames().forEach(k -> servletResponse.setHeader(k, response.getHeader(k)));
        response.getDateHeaderNames().forEach(k -> servletResponse.setDateHeader(k, response.getDateHeader(k)));

        String page = actionInfo.getPage();
        if (Utils.notBlank(page)) {
            page = "/" + request.getApp() + (page.startsWith("/") ? page : "/" + page);
            servletRequest.getRequestDispatcher(page).forward(servletRequest, restContext.resp);
            return;
        }

        String redirect = actionInfo.getRedirect();
        if (Utils.notBlank(redirect)) {
            servletResponse.sendRedirect(RESTController.encodeURL(servletResponse, servletRequest.getContextPath() +
                    DeployerConstants.REST_PREFIX +
                    "/" + request.getView() +
                    "/" + request.getApp() +
                    "/" + request.getModel() +
                    "/" + redirect +
                    "/" + request.getId()));
            return;
        }

        Serializable customizedDataObject = response.getCustomizedDataObject();
        if (customizedDataObject != null) {
            String writeData;
            if (customizedDataObject instanceof String) {
                writeData = (String) customizedDataObject;
            } else {
                Json json = SystemController.getService(Json.class);
                writeData = json.toJson(customizedDataObject);
                restContext.resp.setContentType(JsonView.CONTENT_TYPE);
            }

            PrintWriter writer = restContext.resp.getWriter();
            writer.write(writeData);
            writer.flush();
            return;
        }

        // 作出响应
        String viewName = request.getView();
        View view = views.get(viewName);
        if (view == null) {
            throw new IllegalArgumentException("View not found: " + request.getView());
        }
        if (servletResponse.getContentType() == null) {
            servletResponse.setContentType(view.getContentType());
        }

        if (viewName.equals(HtmlView.FLAG)
                || viewName.equals(JsonView.FLAG)) {
            Map<String, String> dataMap = response.getDataMap();
            if (request.getAction().equals(Show.ACTION_SHOW)) {
                removeNotShow(dataMap, modelInfo);
                orderDataMap(dataMap, modelInfo);
            }
        }

        view.render(restContext);
    }

    private void removeNotShow(Map<String, String> dataMap, ModelInfo modelInfo) {
        Set<String> formFields = new HashSet<>(Arrays.asList(modelInfo.getFormFieldNames()));
        List<String> toRemove = new ArrayList<>();
        dataMap.keySet().forEach(key -> {
            if (!formFields.contains(key)) {
                toRemove.add(key);
                return;
            }

            ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(key);
            if (!fieldInfo.isShow()) {
                toRemove.add(key);
                return;
            }

            if (Utils.notBlank(fieldInfo.getDisplay())) {
                if (!SecurityController.checkRule(fieldInfo.getDisplay(), dataMap::get)) {
                    toRemove.add(key);
                }
            }
        });
        toRemove.forEach(dataMap::remove);
    }

    private void orderDataMap(Map<String, String> dataMap, ModelInfo modelInfo) {
        LinkedHashMap<String, String> orderedData = new LinkedHashMap<>();
        String[] fields = orderedFields(modelInfo);
        for (String field : fields) {
            String found = dataMap.get(field); // 不能用 remove，会修改应用的原始数据结构
            if (found != null) {
                orderedData.put(field, found);
            }
        }
        dataMap.clear();
        dataMap.putAll(orderedData);
    }

    private String[] orderedFields(ModelInfo modelInfo) {
        String[] formFieldNames = modelInfo.getFormFieldNames();
        String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
        String[] fields = new String[formFieldNames.length + monitorFieldNames.length];
        System.arraycopy(formFieldNames, 0, fields, 0, formFieldNames.length);
        System.arraycopy(monitorFieldNames, 0, fields, formFieldNames.length, monitorFieldNames.length);
        return fields;
    }
}
