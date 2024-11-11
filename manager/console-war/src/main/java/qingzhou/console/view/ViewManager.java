package qingzhou.console.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.type.Add;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.type.DownloadView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.Utils;
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
        HttpServletResponse servletResponse = restContext.resp;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());

        if (servletResponse.isCommitted()) return;

        servletResponse.setContentType(response.getContentType());
        response.getHeaderNames().forEach(k -> servletResponse.setHeader(k, response.getHeader(k)));
        response.getDateHeaderNames().forEach(k -> servletResponse.setDateHeader(k, response.getDateHeader(k)));

        String page = actionInfo.getAppPage();
        if (Utils.notBlank(page)) {
            AppPageUtil.doPage(page, restContext);
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
            switch (request.getAction()) {
                case Add.ACTION_CREATE:
                case Update.ACTION_EDIT:
                case Show.ACTION_SHOW:
                case Monitor.ACTION_MONITOR:
                    Map<String, String> dataMap = (Map<String, String>) response.getInternalData();
                    LinkedHashMap<String, String> prepareDataMap = prepareDataMap(dataMap, request);
                    response.setInternalData(prepareDataMap);
                    break;
            }
        }

        view.render(restContext);
    }

    private LinkedHashMap<String, String> prepareDataMap(Map<String, String> dataMap, RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        LinkedHashMap<String, String> orderedData = new LinkedHashMap<>();
        for (String field : modelInfo.getAllFieldNames()) {
            String found = dataMap.get(field); // 不能用 remove，会修改应用的原始数据结构
            if (found == null) continue;

            ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
            if (request.getAction().equals(Show.ACTION_SHOW)) {
                if (!fieldInfo.isShow()) continue;
                if (Utils.notBlank(fieldInfo.getDisplay()))
                    if (!SecurityController.checkRule(fieldInfo.getDisplay(), dataMap::get)) continue;
            }

            orderedData.put(field, found);
        }
        return orderedData;
    }
}
