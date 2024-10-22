package qingzhou.console.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import qingzhou.console.SecurityController;
import qingzhou.console.controller.rest.RestContext;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.console.view.type.StreamView;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.registry.ModelInfo;

public class ViewManager {
    private final Map<String, View> views = new HashMap<>();

    public ViewManager() {
        views.put(HtmlView.FLAG, new HtmlView());
        views.put(JsonView.FLAG, new JsonView());
        views.put(StreamView.FLAG, new StreamView());
    }

    public void render(RestContext restContext) throws Exception {
        RequestImpl request = restContext.request;
        ResponseImpl response = (ResponseImpl) restContext.request.getResponse();

        // 作出响应
        View view = views.get(request.getView());
        if (view == null) {
            throw new IllegalArgumentException("View not found: " + request.getView());
        }

        String contentType = response.getContentType();
        HttpServletResponse servletResponse = restContext.resp;
        servletResponse.setContentType((contentType == null || contentType.isEmpty()) ? view.getContentType() : contentType);

        response.getHeaderNames().forEach(k -> servletResponse.setHeader(k, response.getHeader(k)));
        response.getDateHeaderNames().forEach(k -> servletResponse.setDateHeader(k, response.getDateHeader(k)));

        show(request, response);
        orderResult(request, response);

        view.render(restContext);
    }

    private void show(RequestImpl request, ResponseImpl response) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        Map<String, String> showMap = modelInfo.getShowMap();
        if (showMap.isEmpty()) return;

        List<Map<String, String>> dataList = response.getDataList();
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, String> data = dataList.get(i);
            showMap.entrySet().forEach(entry -> {
                boolean show = SecurityController.checkRule(entry.getValue(), data::get, true);
                if (!show) {
                    data.remove(entry.getKey());
                }
            });
        }
    }

    private void orderResult(RequestImpl request, ResponseImpl response) {
        String[] fields = null;
        List<Map<String, String>> dataList = response.getDataList();
        for (int i = 0; i < dataList.size(); i++) {
            if (fields == null) fields = orderedFields(request.getCachedModelInfo());

            Map<String, String> data = dataList.get(i);
            LinkedHashMap<String, String> orderedData = new LinkedHashMap<>();
            for (String field : fields) {
                String found = data.get(field); // 不能用 remove，会修改应用的原始数据结构
                if (found != null) {
                    orderedData.put(field, found);
                }
            }
            // 添加剩余的不可排序的数据
            data.forEach(orderedData::putIfAbsent);

            dataList.set(i, orderedData);
        }
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
