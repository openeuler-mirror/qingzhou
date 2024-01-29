package qingzhou.console.controller.rest;

import qingzhou.console.I18n;
import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.framework.api.Lang;
import qingzhou.framework.RequestImpl;
import qingzhou.framework.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: 在所有应用 AppStub 内搜索，结果可以显示出 应用名-模块名-属性名/操作名？能否包含 info ?
public class SearchFilter implements Filter<RestContext> {
    public static final String SEARCH_URI = "/search";
    private static final Map<Lang, List<Map<String, String>>> INFOS_FOR_SEARCH = new HashMap<>();

//    static {
//        ModelManager modelManager;
//        for (String modelName : modelManager.getAllModelNames()) {
//            Model model = modelManager.getModel(modelName);
//            Map<String, Map<String, ModelField>> fieldMapWithGroup =;
//            if (!modelManager.getModelFieldMap(modelName).isEmpty()) {
//                fieldMapWithGroup.put("", modelManager.getModelFieldMap(modelName));
//            }
//
//            for (Lang lang : Lang.values()) {
//                List<Map<String, String>> langList = INFOS_FOR_SEARCH.getOrDefault(lang, new ArrayList<>());
//
//                Map<String, String> baseLangMap = new HashMap<>();
//                baseLangMap.put("model", modelName);
//                baseLangMap.put("modelIcon", model.icon());
//                baseLangMap.put("modelName", I18n.getString("model." + modelName, lang));
//                baseLangMap.put("modelInfo", I18n.getString("model.info." + modelName, lang));
//
//                String actionName = null;
//
//                if (modelManager.getModelAction(modelName, "create") != null) {
//                    actionName = "create";
//                } else if (modelManager.getModelAction(modelName, "edit") != null) {
//                    if (!"home".equals(modelName)) {
//                        actionName = "edit";
//                    }
//                }
//                baseLangMap.put("modelAction", actionName == null ? "" : actionName);
//
//                StringBuilder builder = new StringBuilder();
//                ModelAction[] actions = modelManager.getModelActions(modelName);
//                for (ModelAction action : actions) {
//                    builder.append(modelName).append("/").append(action.name()).append(",");
//                }
//                baseLangMap.put("modelActions", builder.length() > 0 ? builder.deleteCharAt(builder.lastIndexOf(",")).toString() : "");
//
//                if (modelManager.getModelAction(modelName, "overview") != null) {
//                    langList.add(baseLangMap);
//                    INFOS_FOR_SEARCH.put(lang, langList);
//                    continue;
//                }
//
//                if (fieldMapWithGroup.isEmpty()) {
////                    if (manager.getModelAction(modelName, MonitorModel.ACTION_NAME_MONITOR) != null)) {
////                        for (MonitorFieldInfo monitoringFieldInfo : modelManager.getMonitoringFieldInfos(modelName).values()) {
////                            if (!MonitorFieldFieldInfo.monitoringField.supportGraphicalDynamic()) {
////                                Map<String, String> fieldMap = new HashMap<>(baseLangMap);
////                                fieldMap.put("modelField", monitoringFieldInfo.name);
////                                fieldMap.put("modelFieldName", I18n.getString("model.field." + modelName + "." + monitoringFieldInfo.name, lang));
////                                fieldMap.put("modelFieldInfo", I18n.getString("model.field.info." + modelName + "." + monitoringFieldInfo.name, lang));
////                                fieldMap.put("modelFieldGroup", "");
////
////                                langList.add(fieldMap);
////                            }
////                        }
////                    } else {
////                        langList.add(baseLangMap);
////                    }
//                } else {
//                    for (String group : fieldMapWithGroup.keySet()) {
//                        for (Map.Entry<String, ModelField> e : fieldMapWithGroup.getOrDefault(group, new LinkedHashMap<>()).entrySet()) {
//                            Map<String, String> fieldMap = new HashMap<>(baseLangMap);
//                            fieldMap.put("modelField", e.getKey());
//                            fieldMap.put("modelFieldName", I18n.getString("model.field." + modelName + "." + e.getKey(), lang));
//                            fieldMap.put("modelFieldInfo", I18n.getString("model.field.info." + modelName + "." + e.getKey(), lang));
//                            fieldMap.put("modelFieldGroup", group);
//
//                            langList.add(fieldMap);
//                        }
//                    }
//                }
//                INFOS_FOR_SEARCH.put(lang, langList);
//            }
//        }
//    }


    @Override
    public boolean doFilter(RestContext context) throws Exception {
        HttpServletRequest request = context.servletRequest;
        HttpServletResponse response = context.servletResponse;
        String checkPath = PageBackendService.retrieveServletPathAndPathInfo(request);
        if (checkPath.equals(SEARCH_URI)) {
            response.setContentType("application/json;charset=UTF-8");
            String user = LoginManager.getLoginUser(request.getSession());
            // note 单词之间是否允许有空格
            String keyword = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
            try (PrintWriter writer = response.getWriter()) {
                if (user != null && !"".equals(keyword)) {
                    RequestImpl qzRequest = (RequestImpl) context.request;
                    writer.write(search(keyword.toLowerCase()).replaceAll("[\\n\\r]", ""));
                } else {
                    writer.write("{\"success\":\"true\",\"msg\":[]}");
                }
                writer.flush();
            }
            return false;
        }

        return true;
    }

    private String search(String keyword) {
        StringBuilder result = new StringBuilder();
        List<Map<String, String>> langList = new HashMap<>(INFOS_FOR_SEARCH).getOrDefault(I18n.getI18nLang(), new ArrayList<>());
        Map<String, List<Map<String, String>>> modelMap = langList.stream().collect(Collectors.groupingBy(map -> map.get("model")));

        Map<String, Boolean> authMap = new HashMap<>();
        modelMap.forEach((k, v) -> {
            for (String action : v.get(0).get("modelActions").split(",")) {
                authMap.put(k, true);
            }
            authMap.put(k, false);
        });

        modelMap.forEach((k, v) -> {
            if (!authMap.get(k)) {
                return;
            }
            boolean fieldMatch = false;
            for (Map<String, String> i : v) {
                if (i.containsKey("modelFieldName") && i.get("modelFieldName").toLowerCase().contains(keyword)) {
                    fieldMatch = true;
                    break;
                }
            }

            if (!fieldMatch) {
                Map<String, String> i = v.get(0);
                if (i.get("modelName").toLowerCase().contains(keyword)) {
                    result.append("{");
                    i.forEach((kk, vv) -> {
                        if (!kk.contains("modelField")) {
                            result.append("\"").append(kk).append("\":\"").append(vv.replaceAll("\"", "\\\\\"")).append("\",");
                        }
                    });
                    result.deleteCharAt(result.lastIndexOf(",")).append("},");
                }
            } else {
                v.stream().filter(i -> i.getOrDefault("modelFieldName", "").toLowerCase().contains(keyword)).forEach(i -> {
                    result.append("{");
                    i.forEach((kk, vv) -> {
                        result.append("\"").append(kk).append("\":\"").append(vv.replaceAll("\"", "\\\\\"")).append("\",");
                    });
                    result.deleteCharAt(result.lastIndexOf(",")).append("},");
                });
            }
        });

        if (result.length() > 0) {
            result.deleteCharAt(result.lastIndexOf(","));
        }

        return "{\"success\":\"true\",\"msg\":[" + result + "]}";
    }

}
