package qingzhou.console.view;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.type.Add;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.controller.rest.SecurityController;
import qingzhou.console.view.type.DownloadView;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

public class ViewManager {
    private static final ViewManager instance = new ViewManager();

    public static ViewManager getInstance() {
        return instance;
    }

    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(HtmlView.FLAG, new HtmlView());
        views.put(JsonView.FLAG, new JsonView());
        views.put(DownloadView.FLAG, new DownloadView());
    }

    public Set<String> getViews() {
        return views.keySet();
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

        int statusCode = response.getStatusCode();
        if (statusCode > 0) {
            servletResponse.setStatus(statusCode);
        }

        String page = actionInfo.getAppPage();
        if (Utils.notBlank(page)) {
            AppPageUtil.doPage(page, restContext);
            return;
        }

        // 作出响应
        String viewName = request.getView();
        View view = views.get(viewName);
        if (view == null) {
            SystemController.getService(Logger.class).error("View not found: " + request.getView());
            return;
        }
        if (servletResponse.getContentType() == null) {
            servletResponse.setContentType(view.getContentType());
        }

        if (response.isSuccess() && (viewName.equals(HtmlView.FLAG) || viewName.equals(JsonView.FLAG))) {
            switch (request.getAction()) {
                case Add.ACTION_CREATE:
                case Update.ACTION_EDIT:
                case Show.ACTION_SHOW:
                case Monitor.ACTION_MONITOR:
                    Serializable internalData = response.getInternalData();
                    if (internalData != null) {
                        LinkedHashMap<String, String> prepareDataMap = prepareDataMap(internalData, request);
                        response.setInternalData(prepareDataMap);
                    }
                    break;
            }
        }

        view.render(restContext);
    }

    private LinkedHashMap<String, String> prepareDataMap(Serializable internalData, RequestImpl request) {
        Map<String, Object> dataMap = (Map<String, Object>) internalData;

        ModelInfo modelInfo = request.getCachedModelInfo();
        LinkedHashMap<String, String> orderedData = new LinkedHashMap<>();
        for (String field : modelInfo.getAllFieldNames()) {
            Object val = dataMap.get(field); // 不能用 remove，会修改应用的原始数据结构
            if (val == null) continue;

            String found = String.valueOf(val);
            ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(field);
            if (request.getAction().equals(Show.ACTION_SHOW)) {
                if (!fieldInfo.isShow()) continue;
                if (Utils.notBlank(fieldInfo.getDisplay()))
                    if (!SecurityController.checkRule(fieldInfo.getDisplay(), key -> String.valueOf(dataMap.get(key))))
                        continue;
            }

            orderedData.put(field, found);
        }
        return orderedData;
    }
}
