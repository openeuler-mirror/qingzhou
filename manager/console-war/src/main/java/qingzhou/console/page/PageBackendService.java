package qingzhou.console.page;

import qingzhou.api.FieldType;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SecurityFilter;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.ViewManager;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Base32Util;
import qingzhou.engine.util.Utils;
import qingzhou.registry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
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

    public static ModelActionInfo renderModelAction(String app, String model, String action, Request request) {
        ModelInfo modelInfo = SystemController.getModelInfo(app, model);
        if (modelInfo == null) return null;
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(action);
        if (actionInfo == null) return null;

        String user = request.getParameterInSession(LoginManager.LOGIN_USER);
        boolean canAccess = SecurityFilter.canAccess(app, model + "/" + action, user);
        return canAccess ? actionInfo : null;
    }

    public static String[] getFieldOptions(String userName, String app, String model, String field) {
        ModelInfo modelInfo = SystemController.getAppInfo(app).getModelInfo(model);
        return modelInfo.getFieldOptions(field);
    }

    public static ModelInfo getModelInfo(Request request) {
        return SystemController.getModelInfo(getAppName(request), request.getModel());
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

    public static String getAppName(Request request) {
        if (request == null) {
            return DeployerConstants.MASTER_APP;
        }

        if ("instance".equals(((RequestImpl) request).getManageType())) {
            return "instance";
        }

        return request.getApp();
    }

    public static String getAppName(String manageType, String appName) {
        if ("instance".equals(manageType)) {
            return "instance";
        }

        return appName;
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
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + RESTController.REST_PREFIX + "/" + viewName + "/" + ((RequestImpl) qzRequest).getManageType() + "/" + qzRequest.getApp() + "/" + model + "/" + action;
        menuBuilder.append("<a href='").append(encodeURL(response, url)).append("' modelName='").append(model).append("'>");
        menuBuilder.append("<i class='icon icon-").append(menu.getMenuIcon()).append("'></i>");
        menuBuilder.append("<span>").append(I18n.getModelI18n(getAppName(qzRequest), "model." + model)).append("</span>");
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
        AppInfo appInfo = SystemController.getAppInfo(getAppName(request));
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

    public static String encodeURL(HttpServletResponse response, String url) {
        return response.encodeURL(url);
    }

    public static void multiSelectGroup(LinkedHashMap<String, String> groupDes, LinkedHashMap<String, LinkedHashMap<String, String>> groupedMap, String options) {
        LinkedHashMap<String, LinkedHashMap<String, String>> tempGroup = new LinkedHashMap<>();
        LinkedHashMap<String, String> twoGroup = new LinkedHashMap<>();
        /*for (Option option : options.options()) {
            String value = option.value();
            String[] groupData = value.split(ConsoleConstants.OPTION_GROUP_SEPARATOR);
            String desc = I18n.getString(option.i18n());
            if (groupData.length == 1) {
                groupDes.putIfAbsent(value, desc);
            } else if (groupData.length == 2) {
                LinkedHashMap<String, String> items = tempGroup.computeIfAbsent(groupData[0], k -> new LinkedHashMap<>());
                items.putIfAbsent(groupData[0], desc);
                twoGroup.putIfAbsent(value, desc);
            } else if (groupData.length == 3) {
                LinkedHashMap<String, String> items = groupedMap.computeIfAbsent(groupData[0] + ConsoleConstants.OPTION_GROUP_SEPARATOR + groupData[1], k -> new LinkedHashMap<>());
                items.put(value, desc);
            }
        }*/
        if (!groupedMap.isEmpty()) {
            groupDes.putAll(twoGroup);
        } else {
            groupedMap.putAll(tempGroup);
        }
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

    public static String getSubmitActionName(Request request) {
        final ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return null;
        }
        boolean isEdit = Objects.equals(DeployerConstants.EDIT_ACTION, request.getAction());
        for (String actionName : modelInfo.getActionNames()) {
            if (actionName.equals(DeployerConstants.UPDATE_ACTION)) {
                if (isEdit) {
                    return DeployerConstants.UPDATE_ACTION;
                }
            } else if (actionName.equals("add")) {
                if (!isEdit) {
                    return "add";
                }
            }
        }

        return isEdit ? DeployerConstants.UPDATE_ACTION : "add";// 兜底
    }

    public static boolean hasIDField(Request request) {
        ModelInfo modelInfo = getModelInfo(request);
        if (modelInfo == null) {
            return false;
        }
        ModelActionInfo listAction = modelInfo.getModelActionInfo("list");
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
        return FieldType.radio.name().equals(fieldType) || FieldType.bool.name().equals(fieldType) || FieldType.select.name().equals(fieldType) || FieldType.groupmultiselect.name().equals(fieldType) || FieldType.checkbox.name().equals(fieldType) || FieldType.sortablecheckbox.name().equals(fieldType);
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
    public static ModelActionInfo[] listCommonOps(Request request, Response response) {
        List<ModelActionInfo> actions = visitActions(request, response.getDataList());
        actions.sort(Comparator.comparingInt(ModelActionInfo::getOrder));

        return actions.toArray(new ModelActionInfo[0]);
    }

    public static ModelActionInfo[] listModelBaseOps(Request request, Map<String, String> obj) {
        List<ModelActionInfo> actions = visitActions(request, new ArrayList<Map<String, String>>() {{
            add(obj);
        }});
        actions.sort(Comparator.comparingInt(ModelActionInfo::getOrder));

        return actions.toArray(new ModelActionInfo[0]);
    }

    private static List<ModelActionInfo> visitActions(Request request, List<Map<String, String>> dataList) {
        final ModelInfo modelInfo = getModelInfo(request);
        List<ModelActionInfo> actions = new ArrayList<>();
        if (modelInfo != null) {
            boolean hasId = hasIDField(request);
            if (hasId && !dataList.isEmpty()) {
                for (String actionName : modelInfo.getBatchActionNames()) {
                    ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
                    boolean isShow = true;
                    for (Map<String, String> data : dataList) {
                        if (SecurityFilter.isActionAvailable(request, data, action) != null || DeployerConstants.EDIT_ACTION.equals(actionName)) {
                            isShow = false;
                            break;
                        }
                    }
                    if (isShow) {
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
        String url = servletRequest.getContextPath() + RESTController.REST_PREFIX + "/" + viewName + "/" + ((RequestImpl) request).getManageType() + "/" + request.getApp() + "/" + request.getModel() + "/" + actionName;
        return response.encodeURL(url);
    }

    /********************* 批量操作 end ************************/

    private static final String encodedFlag = "Encoded:";
    private static final String[] encodeFlags = {
            "#", "?", "&",// 一些不能在url中传递的参数
            ":", "%", "+", " ", "=", ",",
            "[", "]"
    };

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，编码后放在url里作参数，因此需要解码
    public static String decodeId(String encodeId) {
        try {
            if (encodeId.startsWith(encodedFlag)) {
                return new String(Base32Util.decode(encodeId.substring(encodedFlag.length())), StandardCharsets.UTF_8); // for #NC-558 特殊字符可能编码了
            }
        } catch (Exception ignored) {
        }
        return encodeId; // 出错，表示 rest 接口，没有编码
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，不能在url里作参数，因此需要编码
    public static String encodeId(String id) {
        try {
            for (String flag : encodeFlags) {
                if (id.contains(flag)) {
                    return encodedFlag + Base32Util.encode(id.getBytes(StandardCharsets.UTF_8)); // for #NC-558 特殊字符可能编码了
                }
            }
        } catch (Exception ignored) {
        }

        return id; // 出错，表示 rest 接口，没有编码
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
