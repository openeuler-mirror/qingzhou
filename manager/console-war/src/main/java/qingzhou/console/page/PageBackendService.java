package qingzhou.console.page;

import qingzhou.api.FieldType;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.SecurityController;
import qingzhou.console.view.ViewManager;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.Utils;
import qingzhou.registry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 为前端提供展示需要的数据，应包括：master 菜单数据、app菜单数据、表单 group 分组、国际化等
 * 目前是给 jsp 使用，后续可复用给前后端分离的 html 网页
 * 建议多使用 VO 类的对象，可便于后续转换为 json
 */
public class PageBackendService {

    private static final String DEFAULT_EXPAND_MENU_GROUP_NAME = "Service";

    private PageBackendService() {
    }

    public static ModelInfo getModelInfo(Request request) {
        return SystemController.getModelInfo(SystemController.getAppName(request), request.getModel());
    }

    public static String[] getActionNamesShowToList(Request request) {
        ModelInfo modelInfo = getModelInfo(request);
        return Arrays.stream(modelInfo.getModelActionInfos()).filter(modelActionInfo -> modelActionInfo.getOrder() >= 0).
                filter(modelActionInfo -> modelActionInfo.getOrder() > 0)
                .sorted(Comparator.comparingInt(ModelActionInfo::getOrder)).map(ModelActionInfo::getCode).toArray(String[]::new);
    }

    public static String getFieldName(Request request, int fieldIndex) {
        ModelInfo modelInfo = getModelInfo(request);
        return modelInfo.getFormFieldList()[fieldIndex];
    }

    static void printParentMenu(MenuItem menu, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        boolean isDefaultActive = DEFAULT_EXPAND_MENU_GROUP_NAME.equals(menu.getMenuName());
        String model = menu.getMenuName();
        String menuText = I18n.getStringI18n(menu.getI18ns());
        menuBuilder.append("<li class=\"treeview").append(isDefaultActive ? " menu-open expandsub" : "").append(model.equals(curModel) ? " active" : "").append("\">");
        menuBuilder.append("<a href=\"javascript:void(0);\">");
        menuBuilder.append(" <i class=\"icon icon-").append(menu.getMenuIcon()).append("\"></i>");
        menuBuilder.append("<span>").append(menuText).append("</span>");
        menuBuilder.append("<span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
        menuBuilder.append("</a>");
        if (childrenBuilder != null && childrenBuilder.length() > 0) {
            menuBuilder.append("<ul class=\"treeview-menu\">");
            menuBuilder.append(childrenBuilder);
            menuBuilder.append("</ul>");
        }
        menuBuilder.append("</li>");
    }

    static void printChildrenMenu(MenuItem menu, HttpServletRequest request, HttpServletResponse response, String viewName, Request qzRequest, StringBuilder menuBuilder) {
        String model = menu.getMenuName();
        String action = menu.getMenuAction();
        menuBuilder.append("<li class=\"treeview ").append((model.equals(qzRequest.getModel()) ? " active" : "")).append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + qzRequest.getApp() + "/" + model + "/" + action;
        menuBuilder.append("<a href='").append(RESTController.encodeURL(response, url)).append("' modelName='").append(model).append("'>");
        menuBuilder.append("<i class='icon icon-").append(menu.getMenuIcon()).append("'></i>");
        menuBuilder.append("<span>").append(I18n.getModelI18n(SystemController.getAppName(qzRequest), "model." + model)).append("</span>");
        menuBuilder.append("</a>");
        menuBuilder.append("</li>");
    }

    public static String buildMenuHtmlBuilder(HttpServletRequest request, HttpServletResponse response, Request qzRequest) {
        StringBuilder builder = new StringBuilder();
        List<MenuItem> models = getAppMenuList(qzRequest);
        buildMenuHtmlBuilder(models, request, response, ViewManager.htmlView, qzRequest, builder, true);
        String menus = builder.toString();
        return String.format(menus, " ");
    }

    private static void buildMenuHtmlBuilder(List<MenuItem> models, HttpServletRequest request, HttpServletResponse response, String viewName, Request qzRequest, StringBuilder builder, boolean needFavoritesMenu) {
        for (int i = 0; i < models.size(); i++) {
            if (needFavoritesMenu && i == 1) {
                builder.append("%s");
            }
            MenuItem menu = models.get(i);
            StringBuilder childrenBuilder = new StringBuilder();
            List<MenuItem> children = menu.getChildren();
            if (children.isEmpty()) {
                printChildrenMenu(menu, request, response, viewName, qzRequest, childrenBuilder);
                builder.append(childrenBuilder);
            } else {
                StringBuilder parentBuilder = new StringBuilder();
                buildMenuHtmlBuilder(children, request, response, viewName, qzRequest, childrenBuilder, false);
                printParentMenu(menu, qzRequest.getModel(), parentBuilder, childrenBuilder);
                builder.append(parentBuilder);
            }
        }
    }

    public static List<MenuItem> getAppMenuList(Request request) {
        List<MenuItem> menus = new ArrayList<>();
        AppInfo appInfo = SystemController.getAppInfo(SystemController.getAppName(request));
        if (appInfo == null) {
            return menus;
        }

        Map<String, List<ModelInfo>> groupMap = appInfo.getModelInfos().stream()
                .filter(modelInfo -> !modelInfo.isHidden())
                .collect(Collectors.groupingBy(ModelInfo::getMenu));

        groupMap.forEach((menuGroup, models) -> {
            MenuInfo menuData = appInfo.getMenuInfo(menuGroup);
            MenuItem parentMenu = new MenuItem();
            if (menuData != null) {
                parentMenu.setMenuName(menuGroup);
                parentMenu.setMenuIcon(menuData.getIcon());
                parentMenu.setI18ns(menuData.getI18n());
                parentMenu.setOrder(menuData.getOrder());
            }

            models.sort(Comparator.comparingInt(ModelInfo::getOrder));

            models.forEach(modelInfo -> {
                MenuItem subMenu = new MenuItem();
                subMenu.setMenuName(modelInfo.getCode());
                subMenu.setMenuIcon(modelInfo.getIcon());
                subMenu.setI18ns(modelInfo.getName());
                subMenu.setMenuAction(modelInfo.getEntrance());
                subMenu.setOrder(modelInfo.getOrder());
                if (menuData != null) {
                    subMenu.setParentMenu(parentMenu.getMenuName());
                    parentMenu.getChildren().add(subMenu);
                } else {
                    menus.add(subMenu);
                }
            });

            if (menuData != null && !parentMenu.getChildren().isEmpty()) {
                menus.add(parentMenu);
            }
        });
        menus.sort(Comparator.comparingInt(MenuItem::getOrder));

        return menus;
    }

    public static Map<String, Map<String, ModelFieldInfo>> getGroupedModelFieldMap(Request request) {
        Map<String, Map<String, ModelFieldInfo>> result = new LinkedHashMap<>();
        ModelInfo modelInfo = getModelInfo(request);
        for (ModelFieldInfo modelFieldInfo : modelInfo.getModelFieldInfos()) {
            String group = modelFieldInfo.getGroup();
            if (group == null) {
                result.computeIfAbsent("", k -> new LinkedHashMap<>()).put(modelFieldInfo.getCode(), modelFieldInfo);
            } else {
                result.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(modelFieldInfo.getCode(), modelFieldInfo);
            }
        }

        return result;
    }

    public static boolean hasIDField(Request request) {
        ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return false;
        }
        ModelActionInfo listAction = modelInfo.getModelActionInfo(DeployerConstants.ACTION_LIST);
        ModelFieldInfo idField = modelInfo.getModelFieldInfo(modelInfo.getIdFieldName());
        return listAction != null && idField != null;
    }

    public static Map<String, String> modelFieldShowMap(Request request) {
        final ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (ModelFieldInfo e : modelInfo.getModelFieldInfos()) {
            String condition = e.getShow().trim();
            if (!"".equals(condition)) {
                result.put(e.getCode(), condition);
            }
        }

        return result;
    }

    /**
     * list.jsp 在使用
     */
    public static boolean isFilterSelect(Request request, int i) {
        final ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return false;
        }
        ModelFieldInfo[] modelFieldInfos = modelInfo.getModelFieldInfos();
        String fieldType = modelFieldInfos[i].getType();
        return FieldType.radio.name().equals(fieldType) || FieldType.bool.name().equals(fieldType) || FieldType.select.name().equals(fieldType) || FieldType.checkbox.name().equals(fieldType) || FieldType.sortablecheckbox.name().equals(fieldType);
    }

    public static boolean isFieldReadOnly(Request request, String fieldName) {
        final ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return false;
        }
        ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
        if (modelField.getLengthMax() < 1) {
            return true;
        }
        return !modelField.isCreateable() && !modelField.isEditable();
    }

    /********************* 批量操作 start ************************/
    public static ModelActionInfo[] listCommonOps(Request request, Response response, String loginUser) {
        List<ModelActionInfo> actions = visitActions(request, response.getDataList(), loginUser);
        actions.sort(Comparator.comparingInt(ModelActionInfo::getOrder));

        return actions.toArray(new ModelActionInfo[0]);
    }

    public static ModelActionInfo[] listModelBaseOps(Request request, Map<String, String> obj, String loginUser) {
        List<ModelActionInfo> actions = visitActions(request, new ArrayList<Map<String, String>>() {{
            add(obj);
        }}, loginUser);
        actions.sort(Comparator.comparingInt(ModelActionInfo::getOrder));

        return actions.toArray(new ModelActionInfo[0]);
    }

    private static List<ModelActionInfo> visitActions(Request request, List<Map<String, String>> dataList, String loginUser) {
        final ModelInfo modelInfo = getModelInfo(request);
        List<ModelActionInfo> actions = new ArrayList<>();
        if (modelInfo != null) {
            boolean hasId = hasIDField(request);
            if (hasId && !dataList.isEmpty()) {
                for (String actionName : modelInfo.getBatchActionNames()) {
                    boolean isShow = true;
                    for (Map<String, String> data : dataList) {
                        if (!SecurityController.isActionShow(request.getApp(), request.getModel(), actionName, data, loginUser)) {
                            isShow = false;
                            break;
                        }
                    }
                    if (isShow) {
                        ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
                        actions.add(action);
                    }
                }
            }
        }

        return actions;
    }

    //公共操作列表
    public static boolean needOperationColumn(Request request) {
        return getActionNamesShowToList(request).length > 0;
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, Request request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + request.getApp() + "/" + request.getModel() + "/" + actionName;
        return response.encodeURL(url);
    }

    public static Map<String, String> stringToMap(String str) {
        Map<String, String> map = new LinkedHashMap<>();
        if (Utils.isBlank(str)) {
            return map;
        }
        String[] envArr = str.split(",");
        for (String env : envArr) {
            int i = env.indexOf("=");
            if (i < 0) {
                map.put(env, "");
            } else {
                String name = env.substring(0, i);
                String value = env.substring(i + 1);
                map.put(name, value);
            }
        }
        return map;
    }
}
