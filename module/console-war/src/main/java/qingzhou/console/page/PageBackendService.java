package qingzhou.console.page;

import qingzhou.api.*;
import qingzhou.console.*;
import qingzhou.console.controller.AccessControl;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.app.App;
import qingzhou.framework.app.RequestImpl;
import qingzhou.console.util.Base32Util;
import qingzhou.framework.util.StringUtil;
import qingzhou.serialization.ModelActionData;
import qingzhou.serialization.ModelData;
import qingzhou.serialization.ModelFieldData;

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

    public static String[] getActionNamesShowToList(String appName, String modelName) {
        ModelManager modelManager = getModelManager(appName);
        return Arrays.stream(modelManager.getActionNames(modelName)).map(s -> modelManager.getModelAction(modelName, s)).filter(Objects::nonNull).filter(ModelActionData::showToList).sorted(Comparator.comparingInt(ModelActionData::orderOnList)).map(ModelActionData::name).toArray(String[]::new);
    }

    public static String[] getActionNamesShowToListHead(String appName, String modelName) {
        ModelManager modelManager = getModelManager(appName);
        return Arrays.stream(modelManager.getActionNames(modelName)).map(s -> modelManager.getModelAction(modelName, s)).filter(Objects::nonNull).filter(ModelActionData::showToListHead).sorted(Comparator.comparingInt(ModelActionData::orderOnList)).map(ModelActionData::name).toArray(String[]::new);
    }

    public static String getFieldName(String appName, String modelName, int fieldIndex) {
        ModelManager modelManager = getModelManager(appName);
        return modelManager.getFieldNames(modelName)[fieldIndex];
    }

    public static String getInitAppName(RequestImpl request) {
        if (request == null) {
            return App.SYS_APP_MASTER;
        }
        if (ConsoleConstants.MANAGE_TYPE_NODE.equals(request.getManageType())) {
            return App.SYS_APP_NODE_AGENT;
        } else {
            return request.getAppName();
        }
    }

    public static String getMasterAppI18NString(String key, Lang lang) {
        return ConsoleI18n.getI18N(lang, key);
    }

    public static String getMasterAppI18NString(String key) {
        return ConsoleI18n.getI18N(I18n.getI18nLang(), key);
    }

    private static ModelManager getModelManager(String appName) { // 应该只能被 jsp 页面调用
        return ConsoleWarHelper.getAppStub(appName).getModelManager();
    }

    static void printParentMenu(MenuItem menu, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        boolean isDefaultActive = DEFAULT_EXPAND_MENU_GROUP_NAME.equals(menu.getMenuName());
        String model = menu.getMenuName();
        String menuText = I18n.getString(menu.getI18ns());
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

    static void printChildrenMenu(MenuItem menu, HttpServletRequest request, HttpServletResponse response, String viewName, String manageType, String appName, String curModel, StringBuilder menuBuilder) {
        String model = menu.getMenuName();
        String action = menu.getMenuAction();
        menuBuilder.append("<li class=\"treeview ").append((model.equals(curModel) ? " active" : "")).append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + RESTController.REST_PREFIX + "/" + viewName + "/" + manageType + "/" + appName + "/" + model + "/" + action;
        menuBuilder.append("<a href='").append(encodeURL(response, url)).append("' modelName='").append(model).append("'>");
        menuBuilder.append("<i class='icon icon-").append(menu.getMenuIcon()).append("'></i>");
        menuBuilder.append("<span>").append(I18n.getString(appName, "model." + model)).append("</span>");
        menuBuilder.append("</a>");
        menuBuilder.append("</li>");
    }

    public static String buildMenuHtmlBuilder(List<MenuItem> models, HttpServletRequest request, HttpServletResponse response, String viewName, String manageType, String appName, String curModel) {
        StringBuilder builder = new StringBuilder();
        buildMenuHtmlBuilder(models, request, response, viewName, manageType, appName, curModel, builder, true);
        String menus = builder.toString();
        return String.format(menus, " ");
    }

    private static void buildMenuHtmlBuilder(List<MenuItem> models, HttpServletRequest request, HttpServletResponse response, String viewName, String manageType, String appName, String curModel, StringBuilder builder, boolean needFavoritesMenu) {
        for (int i = 0; i < models.size(); i++) {
            if (needFavoritesMenu && i == 1) {
                builder.append("%s");
            }
            MenuItem menu = models.get(i);
            StringBuilder childrenBuilder = new StringBuilder();
            List<MenuItem> children = menu.getChildren();
            if (children.isEmpty()) {
                printChildrenMenu(menu, request, response, viewName, manageType, appName, curModel, childrenBuilder);
                builder.append(childrenBuilder);
            } else {
                StringBuilder parentBuilder = new StringBuilder();
                buildMenuHtmlBuilder(children, request, response, viewName, manageType, appName, curModel, childrenBuilder, false);
                printParentMenu(menu, curModel, parentBuilder, childrenBuilder);
                builder.append(parentBuilder.toString());
            }
        }
    }

    public static List<MenuItem> getAppMenuList(String loginUser, String appName) {
        List<MenuItem> menus = new ArrayList<>();
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return menus;
        }

        ModelData[] allModels = AccessControl.getLoginUserAppMenuModels(loginUser, appName);
        /**
         *  name -> String,
         *  parentName -> name,
         *  entryAction -> String,
         *  order -> int
         *  children -> Properties
         */
        AppStub appStub = ConsoleWarHelper.getAppStub(appName);
        Map<String, List<ModelData>> groupMap = Arrays.stream(allModels).filter(ModelData::showToMenu).collect(Collectors.groupingBy(ModelData::menuName));
        groupMap.forEach((menuGroup, models) -> {
            Menu menu = appStub.getMenu(menuGroup);
            MenuItem parentMenu = new MenuItem();
            if (menu != null) {
                parentMenu.setMenuName(menuGroup);
                parentMenu.setMenuIcon(menu.getIcon());
                parentMenu.setI18ns(menu.getI18n());
                parentMenu.setOrder(menu.getOrder());
            }
            models.sort(Comparator.comparingInt(ModelData::menuOrder));
            models.forEach(i -> {
                MenuItem subMenu = new MenuItem();
                subMenu.setMenuName(i.name());
                subMenu.setMenuIcon(i.icon());
                subMenu.setI18ns(i.nameI18n());
                subMenu.setMenuAction(i.entryAction());
                subMenu.setOrder(i.menuOrder());
                if (menu == null) {
                    menus.add(subMenu);
                } else {
                    subMenu.setParentMenu(parentMenu.getMenuName());
                    parentMenu.getChildren().add(subMenu);
                }
            });
            if (menu != null) {
                menus.add(parentMenu);
            }
        });
        menus.sort(Comparator.comparingInt(MenuItem::getOrder));

        return menus;
    }

    public static String encodeURL(HttpServletResponse response, String url) {
        return response.encodeURL(url);
    }

    public static void multiSelectGroup(LinkedHashMap<String, String> groupDes, LinkedHashMap<String, LinkedHashMap<String, String>> groupedMap, Options options) {
        LinkedHashMap<String, LinkedHashMap<String, String>> tempGroup = new LinkedHashMap<>();
        LinkedHashMap<String, String> twoGroup = new LinkedHashMap<>();
        for (Option option : options.options()) {
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
        }
        if (!groupedMap.isEmpty()) {
            groupDes.putAll(twoGroup);
        } else {
            groupedMap.putAll(tempGroup);
        }
    }

    public static Map<String, Map<String, ModelFieldData>> getGroupedModelFieldMap(Request request) {
        Map<String, Map<String, ModelFieldData>> result = new LinkedHashMap<>();
        ModelManager manager = getModelManager(request.getAppName());
        String modelName = request.getModelName();
        for (String groupName : manager.getGroupNames(modelName)) {
            Map<String, ModelFieldData> map = new LinkedHashMap<>();
            String[] fieldNamesByGroup = manager.getFieldNamesByGroup(modelName, groupName);
            for (String f : fieldNamesByGroup) {
                ModelFieldData modelField = manager.getModelField(modelName, f);
                map.put(f, modelField);
            }
            result.put(groupName, map);
        }

        return result;
    }

    public static String getSubmitActionName(Request request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        boolean isEdit = Objects.equals(EditModel.ACTION_NAME_EDIT, request.getActionName());
        for (String actionName : modelManager.getActionNames(request.getModelName())) {
            if (actionName.equals(EditModel.ACTION_NAME_UPDATE)) {
                if (isEdit) {
                    return EditModel.ACTION_NAME_UPDATE;
                }
            } else if (actionName.equals(AddModel.ACTION_NAME_ADD)) {
                if (!isEdit) {
                    return AddModel.ACTION_NAME_ADD;
                }
            }
        }
        return isEdit ? EditModel.ACTION_NAME_UPDATE : AddModel.ACTION_NAME_ADD;// 兜底
    }

    public static boolean hasIDField(Request request) {
        RequestImpl requestImpl = (RequestImpl) request;
        String appName = request.getAppName();
        if (requestImpl.getManageType().equals(ConsoleConstants.MANAGE_TYPE_NODE)) {
            appName = App.SYS_APP_NODE_AGENT;
        }
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return false;
        }
        ModelActionData listAction = modelManager.getModelAction(request.getModelName(), ListModel.ACTION_NAME_LIST);
        ModelFieldData idField = modelManager.getModelField(request.getModelName(), ListModel.FIELD_NAME_ID);
        return listAction != null && idField != null;
    }

    public static String isActionEffective(Request request, Map<String, String> obj, ModelActionData modelAction) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        if (modelAction != null) {
            String effectiveWhen = modelAction.effectiveWhen().trim();
            boolean effective = false;
            try {
                effective = Validator.isEffective(obj::get, effectiveWhen);
            } catch (Exception ignored) {
            }
            if (!effective) {
                return String.format(
                        ConsoleI18n.getI18N(I18n.getI18nLang(), "validator.ActionEffective.notsupported"),
                        I18n.getString(request.getAppName(), "model.action." + request.getModelName() + "." + request.getActionName()),// todo
                        effectiveWhen);
            }
        }
        return null;
    }

    public static Map<String, String> modelFieldEffectiveWhenMap(Request request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        String modelName = request.getModelName();
        for (String fieldName : modelManager.getFieldNames(modelName)) {
            ModelFieldData modelField = modelManager.getModelField(modelName, fieldName);
            String condition = modelField.effectiveWhen();
            if (StringUtil.notBlank(condition)) {
                result.put(fieldName, condition);
            }
        }

        return result;
    }

    /**
     * list.jsp 在使用
     */
    public static boolean isFilterSelect(Request request, int i) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        String modelName = request.getModelName();
        String[] allFieldNames = modelManager.getFieldNames(modelName);
        ModelFieldData modelField = modelManager.getModelField(modelName, allFieldNames[i]);
        FieldType fieldType = modelField.type();
        return fieldType == FieldType.radio || fieldType == FieldType.bool || fieldType == FieldType.select || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox;
    }

    public static boolean isFieldReadOnly(Request request, String fieldName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelFieldData modelField = modelManager.getModelField(request.getModelName(), fieldName);
        if (modelField.maxLength() < 1) {
            return true;
        }
        return modelField.disableOnCreate() && modelField.disableOnEdit();
    }

    public static boolean isAjaxAction(String actionName) {
        return EditModel.ACTION_NAME_UPDATE.equals(actionName) ||
                AddModel.ACTION_NAME_ADD.equals(actionName) ||
                DeleteModel.ACTION_NAME_DELETE.equals(actionName);
    }

    /********************* 批量操作 start ************************/
    public static ModelActionData[] listCommonOps(Request request, Response response) {
        List<ModelActionData> actions = visitActions(request, response.getDataList());
        actions.sort(Comparator.comparingInt(ModelActionData::orderOnList));

        return actions.toArray(new ModelActionData[0]);
    }

    public static ModelActionData[] listModelBaseOps(Request request, Map<String, String> obj) {
        List<ModelActionData> actions = visitActions(request, new ArrayList<Map<String, String>>() {{
            add(obj);
        }});
        actions.sort(Comparator.comparingInt(ModelActionData::orderOnList));

        return actions.toArray(new ModelActionData[0]);
    }

    private static List<ModelActionData> visitActions(Request request, List<Map<String, String>> dataList) {
        final String appName = request.getAppName();
        final ModelManager modelManager = getModelManager(appName);
        List<ModelActionData> actions = new ArrayList<>();
        if (modelManager != null) {
            final String modelName = request.getModelName();
            boolean hasId = hasIDField(request);
            if (hasId && !dataList.isEmpty()) {
                for (String ac : modelManager.getActionNamesSupportBatch(modelName)) {
                    ModelActionData action = modelManager.getModelAction(modelName, ac);
                    boolean isShow = true;
                    for (Map<String, String> data : dataList) {
                        final String actionName = action.name();
                        if (isActionEffective(request, data, action) != null || EditModel.ACTION_NAME_EDIT.equals(actionName) || !action.showToList()) {
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
        final String appName = request.getAppName();
        final ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return false;
        }

        return getActionNamesShowToList(appName, request.getModelName()).length > 0;
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, RequestImpl request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + RESTController.REST_PREFIX + "/" + viewName + "/" + request.getManageType() + "/" + request.getAppName() + "/" + request.getModelName() + "/" + actionName;
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
}
