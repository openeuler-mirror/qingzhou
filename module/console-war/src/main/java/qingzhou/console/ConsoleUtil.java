package qingzhou.console;

import qingzhou.api.AppContext;
import qingzhou.api.console.FieldType;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.ModelManager;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.group.GroupManager;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ModelBase;
import qingzhou.api.console.option.Option;
import qingzhou.api.console.option.OptionManager;
import qingzhou.console.auth.AccessControl;
import qingzhou.console.controller.RESTController;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.login.LoginManager;
import qingzhou.console.sec.Encryptor;
import qingzhou.console.sec.SecureKey;
import qingzhou.console.util.Constants;
import qingzhou.console.util.ObjectUtil;
import qingzhou.console.util.StringUtil;
import qingzhou.framework.app.I18n;
import qingzhou.framework.impl.app.ConsoleContextImpl;
import qingzhou.framework.pattern.Visitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

public class ConsoleUtil {
    public static String ACTION_NAME_SERVER = "server";
    public static String ACTION_NAME_TARGET = "target";
    public static String ACTION_NAME_key = "key";
    public static String ACTION_NAME_validate = "validate";
    public static String ACTION_NAME_addfavorite = "addfavorite";
    public static String ACTION_NAME_cancelfavorites = "cancelfavorites";
    public static String TARGET_TYPE_SET_FLAG = "targetType";
    public static String TARGET_NAME_SET_FLAG = "targetName";
    public static String GROUP_NAME_PRODUCT = "product";
    public static String GROUP_NAME_LICENSE = "license";
    private static Boolean disableUpload;
    private static Boolean disableDownload;

    private ConsoleUtil() {
    }

    public static void error(String msg, Throwable t) {
        ConsoleWarHelper.getLogger().error(msg, t);
    }

    public static void warn(String msg) {
        ConsoleWarHelper.getLogger().warn(msg);
    }

    public static String getAppName(String targetType, String targetName) {
        return ServerXml.getAppName(targetType, targetName);
    }

    public static ModelManager getModelManager(String appName) {
        return getAppContext(appName).getConsoleContext().getModelManager();
    }

    static void printParentMenu(Properties menu, String targetType, String targetName, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        String model = menu.getProperty("name");
        menuBuilder.append("<li class=\"treeview" + (model.equals(curModel) ? " active" : "") + "\">");
        menuBuilder.append("<a href=\"javascript:void(0);\">");
        menuBuilder.append(" <i class=\"icon icon-" + menu.getProperty("icon") + "\"></i>");
        ConsoleContextImpl consoleContext = (ConsoleContextImpl) getAppContext(null).getConsoleContext();
        String menuText = "未分类";
        if (StringUtil.notBlank(model)) {
            ConsoleContextImpl.MenuInfo menuInfo = consoleContext.getMenuInfo(model);
            if (menuInfo == null) {
                // todo
//                menuInfo = ((ConsoleContextImpl) Main.getInternalService(ConsoleContextFinder.class).find(Constants.QINGZHOU_DEFAULT_APP_NAME)).getMenuInfo(model);
            }
            if (menuInfo != null) {
                menuText = I18n.getString(menuInfo.getMenuI18n());
            }
        }
        menuBuilder.append("<span>" + menuText + "</span>");
        menuBuilder.append("<span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
        menuBuilder.append("</a>");
        if (childrenBuilder != null && childrenBuilder.length() > 0) {
            menuBuilder.append("<ul class=\"treeview-menu\">");
            menuBuilder.append(childrenBuilder);
            menuBuilder.append("</ul>");
        }
        menuBuilder.append("</li>");
    }

    static void printChildrenMenu(Properties menu, HttpServletRequest request, HttpServletResponse response, String viewName, String targetType, String targetName, String curModel, StringBuilder menuBuilder) {
        String model = menu.getProperty("name");
        String action = menu.getProperty("entryAction");
        menuBuilder.append("<li class=\"treeview " + (model.equals(curModel) ? " active" : "") + "\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + RESTController.REST_PREFIX + "/" + viewName + "/" + targetType + "/" + targetName + "/" + model + "/" + action;
        menuBuilder.append("<a href='" + ConsoleUtil.encodeURL(request, response, url) + "' modelName='" + model + "'>");
        menuBuilder.append("<i class='icon icon-" + menu.getProperty("icon") + "'></i>");
        menuBuilder.append("<span>" + I18n.getString(getAppName(targetType, targetName), "model." + model) + "</span>");
        menuBuilder.append("</a>");
        menuBuilder.append("</li>");
    }

    static String printFavoritesMenu(String loginUser, HttpServletRequest request, HttpServletResponse response, String viewName, String targetType, String targetName) {
        StringBuilder menuBuilder = new StringBuilder();
        List<String> myFavorites = ServerXml.getInstanceFavorites(loginUser, targetName);
        if (myFavorites.size() > 0) {
            menuBuilder.append("<li class=\"treeview\">");
            menuBuilder.append("<a href=\"javascript:void(0);\">");
            menuBuilder.append(" <i class=\"icon icon-star\"></i>");
            ConsoleContextImpl consoleContext = (ConsoleContextImpl) ConsoleUtil.getAppContext(null).getConsoleContext();
            ConsoleContextImpl.MenuInfo menuInfo = consoleContext.getMenuInfo("Favorites");
            String menuText = I18n.getString(menuInfo.getMenuI18n());
            menuBuilder.append("<span>" + menuText + "</span>");
            menuBuilder.append("<span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
            menuBuilder.append("</a>");
            menuBuilder.append("<ul class=\"treeview-menu\">");
            for (String myFavorite : myFavorites) {
                String[] favorites = myFavorite.split("/");
                if (favorites.length == 3) {
                    String instanceName = favorites[0];
                    ModelManager modelManager = consoleContext.getModelManager();
                    String modelName = favorites[1];
                    String actionName = favorites[2];
                    ModelAction modelAction = modelManager.getModelAction(modelName, actionName);
                    menuBuilder.append("<li class=\"treeview\">");
                    menuBuilder.append("<a href='" + ConsoleUtil.encodeURL(request, response, viewName + "/" + targetType + "/" + instanceName + "/" + modelName + "/" + actionName) + "' modelName='" + modelName + "'>");
                    menuBuilder.append("<i class='icon icon-" + modelAction.icon() + "'></i>");
                    menuBuilder.append("<span>" + I18n.getString(getAppName(targetType, targetName), "model." + modelName) + "</span>");
                    menuBuilder.append("</a>");
                    menuBuilder.append("</li>");
                }
            }
            menuBuilder.append("</ul>");
            menuBuilder.append("</li>");
        }

        return menuBuilder.toString();
    }

    public static String buildMenuHtmlBuilder(List<Properties> models, String loginUser, HttpServletRequest request, HttpServletResponse response, String viewName, String targetType, String targetName, String curModel) {
        StringBuilder builder = new StringBuilder();
        buildMenuHtmlBuilder(models, request, response, viewName, targetType, targetName, curModel, builder, true);
        String menus = builder.toString();
        String favoritesMenu = printFavoritesMenu(loginUser, request, response, viewName, targetType, targetName);
        return String.format(menus, StringUtil.isBlank(favoritesMenu) ? " " : favoritesMenu);
    }

    private static void buildMenuHtmlBuilder(List<Properties> models, HttpServletRequest request, HttpServletResponse response, String viewName, String targetType, String targetName, String curModel, StringBuilder builder, boolean needFavoritesMenu) {
        models.sort(java.util.Comparator.comparing(o -> String.valueOf(o.get("order"))));

        for (int i = 0; i < models.size(); i++) {
            if (needFavoritesMenu && i == 1) {
                builder.append("%s");
            }
            Properties menu = models.get(i);
            if (Constants.MODEL_NAME_favorites.equals(menu.getProperty("name"))) {
                continue;
            }
            StringBuilder childrenBuilder = new StringBuilder();
            Object c = menu.get("children");
            if (c == null) {
                printChildrenMenu(menu, request, response, viewName, targetType, targetName, curModel, childrenBuilder);
                builder.append(childrenBuilder);
            } else {
                List<Properties> childrenMenus = (List<Properties>) c;
                StringBuilder parentBuilder = new StringBuilder();
                buildMenuHtmlBuilder(childrenMenus, request, response, viewName, targetType, targetName, curModel, childrenBuilder, false);
                printParentMenu(menu, targetType, targetName, curModel, parentBuilder, childrenBuilder);
                builder.append(parentBuilder.toString());
            }

        }
    }

    // todo 临时，仅支持单应用
    public static AppContext getAppContext(String appName) {
        return ConsoleWarHelper.getAppContext(appName);
    }

    public static List<Properties> getAppMenuList(String loginUser, String appName) {
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return new ArrayList<>();
        }

        Model[] allModels = AccessControl.getLoginUserAppMenuModels(loginUser, appName);
        /**
         *  name -> String,
         *  parentName -> name,
         *  entryAction -> String,
         *  order -> int
         *  children -> Properties
         */
        ConsoleContextImpl consoleContext = (ConsoleContextImpl) getAppContext(null).getConsoleContext();
        Map<String, Properties> modelMap = new HashMap<>();
        for (Model model : allModels) {
            String modelName = model.name();
            Properties menu = new Properties();
            menu.put("name", modelName);
            menu.put("icon", model.icon());
            menu.put("order", model.menuOrder());
            menu.put("entryAction", model.entryAction());
            String menuName = model.menuName();
            if (StringUtil.notBlank(menuName)) {
                ConsoleContextImpl.MenuInfo menuInfo = consoleContext.getMenuInfo(menuName);
                if (menuInfo == null) {
                    menuInfo = consoleContext.getMenuInfo(menuName);
                    if (menuInfo == null) {
                        continue;
                    }
                }
                menu.put("parentName", menuName);

                Properties parentMenu = new Properties();
                parentMenu.put("name", menuName);
                parentMenu.put("icon", menuInfo.getMenuIcon());
                parentMenu.put("order", menuInfo.getMenuOrder());
                modelMap.putIfAbsent(menuName, parentMenu);
            }
            modelMap.putIfAbsent(modelName, menu);
        }

        return buildMenuTree(modelMap);
    }

    private static List<Properties> buildMenuTree(Map<String, Properties> modelMap) {
        List<Properties> tree = new ArrayList<>();
        // 创建一个根节点列表，这些节点的parentname为空。
        for (Properties model : modelMap.values()) {
            if (StringUtil.isBlank(model.getProperty("parentName"))) {
                tree.add(model);
            } else {
                // 如果节点的parentname不为空，那么将该节点添加到其父节点的children列表中。
                Properties parent = modelMap.get(model.getProperty("parentName"));
                List<Properties> children;
                if (parent != null) {
                    if (parent.get("children") == null) {
                        children = new ArrayList<>();
                        parent.put("children", children);
                    } else {
                        children = ((List) parent.get("children"));
                    }
                    children.add(model);
                }
            }
        }

        return tree;
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, RequestImpl request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + RESTController.REST_PREFIX + "/" + viewName + "/" + request.getAppName() + "/" + request.getModelName() + "/" + actionName;
        return response.encodeURL(url);
    }

    public static ModelAction[] getShowToListActions(RequestImpl request) {
        List<String> actions = actionsToList(request);
        List<ModelAction> modelActions = new ArrayList<>();

        if (actions != null) {
            ModelManager modelManager = getModelManager(request.getAppName());
            for (String acName : actions) {
                ModelAction action = modelManager.getModelAction(request.getModelName(), acName);
                if (action != null) {
                    modelActions.add(action);
                }
            }
        }

        return modelActions.toArray(new ModelAction[0]);
    }

    public static List<Map<String, String>> listModels(HttpServletRequest request, String targetType, String targetName, String modelName) {
        String appName = getAppName(targetType, targetName);
       /* ModelManagerImpl modelManager = (ModelManagerImpl) getModelManager(appName); todo
        if (modelManager == null) {
            return new ArrayList<>();
        }
        Class<?> modelClass = modelManager.getModelClass(modelName);
        if (modelClass == null) {
            return new ArrayList<>();
        }

        Request qzRequest = buildRequest(request, targetType, targetName, appName, modelName, "list", null);
        ResponseImpl response = new ResponseImpl();
        try {
            modelManager.invokeAction(qzRequest, response);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return new ArrayList<>();
    }

    public static GroupManager fieldGroups(RequestImpl request, String groupName) {
        String appName = request.getAppName();
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return null;
        }
        ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());
        return modelInstance.fieldGroups(groupName);
    }

    public static OptionManager fieldOptions(RequestImpl request, String fieldName) {
        String appName = request.getAppName();
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return null;
        }
        ModelBase modelBase = modelManager.getModelInstance(request.getModelName());
        return modelBase.fieldOptions(request, fieldName);
    }

    public static boolean isListModel(String appName, String modelName) {
        ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return false;
        }
        return ListModel.class.isAssignableFrom(modelManager.getModelInstance(modelName).getClass());
    }

    public static void multiSelectGroup(LinkedHashMap<String, String> groupDes,
                                        LinkedHashMap<String, LinkedHashMap<String, String>> groupedMap,
                                        OptionManager optionsManager) {
        LinkedHashMap<String, LinkedHashMap<String, String>> tempGroup = new LinkedHashMap<>();
        LinkedHashMap<String, String> twoGroup = new LinkedHashMap<>();
        for (Option option : optionsManager.options()) {
            String value = option.value();
            String[] groupData = value.split(Constants.GROUP_SEPARATOR);
            String desc = I18n.getString(option.i18n());
            if (groupData.length == 1) {
                groupDes.putIfAbsent(value, desc);
            } else if (groupData.length == 2) {
                LinkedHashMap<String, String> items = tempGroup.computeIfAbsent(groupData[0], k -> new LinkedHashMap<>());
                items.putIfAbsent(groupData[0], desc);
                twoGroup.putIfAbsent(value, desc);
            } else if (groupData.length == 3) {
                LinkedHashMap<String, String> items = groupedMap.computeIfAbsent(groupData[0] + Constants.GROUP_SEPARATOR + groupData[1], k -> new LinkedHashMap<>());
                items.put(value, desc);
            }
        }
        if (groupedMap.size() > 0) {
            groupDes.putAll(twoGroup);
        } else {
            groupedMap.putAll(tempGroup);
        }
    }

    /**
     * 属性是否符合有效性规则
     */
    public static boolean isEffective(Validator.FieldValueRetriever retriever, String effectiveWhen) throws Exception {
        if (StringUtil.isBlank(effectiveWhen)) {
            return true;
        }
        return Validator.isEffective(retriever, effectiveWhen);
    }


    public static boolean actionsWithAjax(RequestImpl request, String actionName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelBase model = modelManager.getModelInstance(request.getModelName());
        for (String ajaxAction : model.actionsWithAjax()) {
            if (ajaxAction.equals(actionName)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> actionsToList(RequestImpl request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        ModelBase model = modelManager.getModelInstance(request.getModelName());
        return model.actionsToList();
    }

    public static boolean actionShowToList(RequestImpl request, String actionName) {
        List<String> strings = actionsToList(request);
        return strings != null && strings.contains(actionName);
    }

    public static boolean actionShowToListHead(RequestImpl request, String actionName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelBase model = modelManager.getModelInstance(request.getModelName());
        for (String a : model.actionsToListHead()) {
            if (a.equals(actionName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBatchAction(String appName, String modelName, String actionName) {
        final ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return false;
        }
        ModelBase model = modelManager.getModelInstance(modelName);
        for (String a : model.actionsSupportBatch()) {
            if (a.equals(actionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * list.jsp 在使用
     */
    public static boolean isFilterSelect(RequestImpl request, int i) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        String modelName = request.getModelName();
        String[] allFieldNames = modelManager.getAllFieldNames(modelName);
        ModelField modelField = modelManager.getModelField(modelName, allFieldNames[i]);
        FieldType fieldType = modelField.type();
        return fieldType == FieldType.radio || fieldType == FieldType.bool || fieldType == FieldType.select || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox;
    }

    public static boolean isFieldReadOnly(RequestImpl request, String fieldName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelField modelField = modelManager.getModelField(request.getModelName(), fieldName);
        if (modelField.maxLength() < 1) {
            return true;
        }
        if (!modelField.effectiveOnCreate() && !modelField.effectiveOnEdit()) {
            return true;
        }

        ModelBase modelBase = modelManager.getModelInstance(request.getModelName());
        return modelBase.isReadOnly(request, fieldName);
    }

    public static boolean hasIDField(RequestImpl request) {
        try {
            final ModelManager modelManager = getModelManager(request.getAppName());
            if (modelManager == null) {
                return false;
            }
            ModelField modelField = modelManager.getModelField(request.getModelName(), ListModel.FIELD_NAME_ID);
            if (modelField != null) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static String isActionEffective(RequestImpl request, Map<String, String> obj, ModelAction modelAction) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        if (modelAction != null) {
            String effectiveWhen = modelAction.effectiveWhen().trim();
            boolean effective = false;
            try {
                effective = isEffective(fieldName -> obj.get(fieldName), effectiveWhen);
            } catch (Exception ignored) {
            }
            if (!effective) {
                return String.format(
                        I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "validator.ActionEffective.notsupported"),
                        I18n.getString(request.toString(), "model.action." + request.getModelName() + "." + request.getActionName()),// todo
                        effectiveWhen);
            }
        }
        ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());

        return modelInstance.actionNotEffective(request);
    }

    public static boolean isDisableUpload() {
        if (disableUpload == null) {
            disableUpload = ServerXml.get().isDisableUpload();
        }

        return disableUpload;
    }

    public static boolean isDisableDownload() {
        if (disableDownload == null) {
            disableDownload = ServerXml.get().isDisableDownload();
        }

        return disableDownload;
    }

    public static Map<String, String> modelFieldEffectiveWhenMap(RequestImpl request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        String modelName = request.getModelName();
        for (String fieldName : modelManager.getAllFieldNames(modelName)) {
            ModelField modelField = modelManager.getModelField(modelName, fieldName);
            String condition = modelField.effectiveWhen();
            if (StringUtil.notBlank(condition)) {
                result.put(fieldName, condition);
            }
        }

        return result;
    }

    public Map<String, List<String>> getInRefField(String modelName) {
        Map<String, List<String>> inRefModelField = new HashMap<>();
//        for (ModelInfo mi : modelInfoMap.values()) {
//            for (FieldInfo fieldInfo : mi.fieldInfoMap.values()) {
//                String refModel = fieldInfo.modelField.refModel();
//                if (StringUtil.notBlank(refModel)) {
//                    if (refModel.equals(modelName)) {
//                        List<String> fields = inRefModelField.computeIfAbsent(mi.model.name(), model -> new ArrayList<>());
//                        fields.add(fieldInfo.field.getName());
//                    }
//                }
//            }
//        }
        return inRefModelField;
    }

    public static LinkedHashMap<String, ModelField> getModelFieldMap(RequestImpl request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return new LinkedHashMap<>();
        }
        LinkedHashMap<String, ModelField> map = new LinkedHashMap<>();
        Map<String, ModelField> modelFields = modelManager.getModelFieldMap(request.getModelName());
        for (Map.Entry<String, ModelField> e : modelFields.entrySet()) {
            map.put(e.getKey(), e.getValue());
        }

        return map;
    }

    public static String getSubmitActionName(RequestImpl request) {
        boolean isEdit = Objects.equals("edit", request.getActionName());
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return null;
        }
        for (ModelAction modelAction : modelManager.getModelActions(request.getModelName())) {
            String actionName = modelAction.name();
            if (actionName.equals("update")) {
                if (isEdit) {
                    return "update";
                }
            } else if (actionName.equals("add")) {
                if (!isEdit) {
                    return "add";
                }
            }
        }
        return isEdit ? "update" : "add";// 兜底
    }

    public static List<ModelBase> convertMapToModelBase(RequestImpl request, Response response) {
        List<ModelBase> modelBases = new ArrayList<>();
        final List<Map<String, String>> models = response.modelData().getDataList();
        if (models != null) {
            final ModelManager modelManager = getModelManager(request.getAppName());
            if (modelManager == null) {
                return null;
            }
            for (Map<String, String> model : models) {
                ModelBase modelInstance = modelManager.getModelInstance(request.getModelName());
                try {
                    ObjectUtil.setObjectValues(modelInstance, model);
                    modelBases.add(modelInstance);
                } catch (Exception ignored) {
                }
            }
        }

        return modelBases;
    }

    /********************* 批量操作 start ************************/
    //公共操作列表
    public static boolean needOperationColumn(RequestImpl request, Response response, HttpSession session) throws Exception {
        final boolean[] needOperationColumn = {false};
        visitActions(request, response, session,
                obj -> {
                    needOperationColumn[0] = true;
                    return false;
                });
        return needOperationColumn[0];
    }

    private static void visitActions(RequestImpl request, Response response, HttpSession session, Visitor<ModelAction> visitor) throws Exception {
        visitActions(request, response, session, visitor, response.modelData().getDataList(), true);
    }

    private static void visitActions(RequestImpl request, Response response, HttpSession session, Visitor<ModelAction> visitor, List<Map<String, String>> datas, boolean checkEffective) throws Exception {
        final String appName = request.getAppName();
        final ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return;
        }
        final String modelName = request.getModelName();
        boolean hasId = hasIDField(request);
        if (hasId && !response.modelData().getDataList().isEmpty()) {
            ModelAction[] actions = modelManager.getModelActions(modelName);
            ModelBase modelBase = modelManager.getModelInstance(modelName);
            for (ModelAction action : actions) {
                for (Map<String, String> data : datas) {
                    if (checkEffective && isActionEffective(request, data, action) != null) {
                        continue;
                    }

                    final String actionName = action.name();
                    if (EditModel.ACTION_NAME_EDIT.equals(actionName)) {
                        continue;
                    }

                    if (!actionShowToList(request, actionName) || actionShowToListHead(request, actionName)) {
                        continue;
                    }

                    if (!AccessControl.canAccess(null, null, modelName + "/" + actionName, LoginManager.getLoginUser(session))) {
                        continue;
                    }

                    if (modelBase.actionNotEffective(request) != null) {
                        continue;
                    }

                    if (!isBatchAction(appName, modelName, action.name())) {
                        continue;
                    }

                    if (!visitor.visit(action)) {
                        break;
                    }
                }
            }
        }
    }

    public static ModelAction[] listCommonOps(RequestImpl request, Response response, HttpSession session) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, session, batchVisitor);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    public static ModelAction[] listModelBaseOps(RequestImpl request, Response response, HttpSession session, Map<String, String> obj) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, session, batchVisitor, new ArrayList<Map<String, String>>() {{
            add(obj);
        }}, true);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    private static class BatchVisitor implements Visitor<ModelAction> {
        List<ModelAction> modelActions = new ArrayList<>();

        @Override
        public boolean visit(ModelAction action) {
            if (!modelActions.contains(action)) {
                modelActions.add(action);
            }

            return true;
        }
    }

    /********************* 批量操作 end ************************/


    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }

    public static String encodeRedirectURL(HttpServletRequest request, HttpServletResponse response, String url) {
        return response.encodeURL(encodeTarget(request, url));
    }

    public static String encodeURL(HttpServletRequest request, HttpServletResponse response, String url) {// 应该优先考虑使用 非静态 的同名方法，而不是这个
        return response.encodeURL(encodeTarget(request, url));
    }

    private static String encodeTarget(HttpServletRequest request, String url) {
        String type = (String) request.getAttribute(TARGET_TYPE_SET_FLAG);
        String name = (String) request.getAttribute(TARGET_NAME_SET_FLAG);
        if (StringUtil.isBlank(type) || StringUtil.isBlank(name)
//                || isCentralizedUrl(url) todo 集中管理的url需要标识，以不用 wrap url
        ) {
            return url;
        }

        String path = url;
        String query = "";
        String anchor = "";
        int pound = path.indexOf('#');
        if (pound >= 0) {
            anchor = path.substring(pound);
            path = path.substring(0, pound);
        }
        int question = path.indexOf('?');
        if (question >= 0) {
            query = path.substring(question);
            path = path.substring(0, question);
        }
        StringBuilder sb = new StringBuilder(path);
        if (query.length() > 0) {
            sb.append(query);
            sb.append('&');
        } else {
            sb.append('?');
        }

        // 扩充的内容 begin
        sb.append(TARGET_TYPE_SET_FLAG);
        sb.append('=');
        sb.append(type);
        sb.append('&');
        sb.append(TARGET_NAME_SET_FLAG);
        sb.append('=');
        sb.append(name);
        // 扩充的内容 end

        sb.append(anchor);
        return sb.toString();
    }

    public static String retrieveServletPathAndPathInfo(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }

    public static String getConsolePublicKey() {
        return Encryptor.getPublicKeyString();
    }

    public static String decryptWithConsolePrivateKey(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }
        try {
            return ConsoleWarHelper.getCryptoService().getPublicKeyCipher(
                    SecureKey.getSecureKey(ConsoleWarHelper.getDomain(), SecureKey.publicKeyName),
                    SecureKey.getSecureKey(ConsoleWarHelper.getDomain(), SecureKey.privateKeyName)
            ).decryptWithPrivateKey(input);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public static String checkPwd(String password, String... infos) {
        if (Constants.PASSWORD_FLAG.equals(password)) {
            return null;
        }

        int minLength = 10;
        int maxLength = 20;
        if (password.length() < minLength || password.length() > maxLength) {
            return String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "validator.lengthBetween"), minLength, maxLength);
        }

        if (infos != null && infos.length > 0) {
            if (infos[0] != null) { // for #ITAIT-5014
                if (password.contains(infos[0])) { // 包含身份信息
                    return I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "password.passwordContainsUsername");
                }
            }
        }

        //特殊符号包含下划线
        String PASSWORD_REGEX = "^(?![A-Za-z0-9]+$)(?![a-z0-9_\\W]+$)(?![A-Za-z_\\W]+$)(?![A-Z0-9_\\W]+$)(?![A-Z0-9\\W]+$)[\\w\\W]{10,}$";
        if (!Pattern.compile(PASSWORD_REGEX).matcher(password).matches()) {
            return I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "password.format");
        }

        if (isContinuousChar(password)) { // 连续字符校验
            return I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "password.continuousChars");
        }

        return null;
    }

    /**
     * 是否包含3个及以上相同或字典连续字符
     */
    private static boolean isContinuousChar(String password) {
        char[] chars = password.toCharArray();
        for (int i = 0; i < chars.length - 2; i++) {
            int n1 = chars[i];
            int n2 = chars[i + 1];
            int n3 = chars[i + 2];
            // 判断重复字符
            if (n1 == n2 && n1 == n3) {
                return true;
            }
            // 判断连续字符： 正序 + 倒序
            if ((n1 + 1 == n2 && n1 + 2 == n3) || (n1 - 1 == n2 && n1 - 2 == n3)) {
                return true;
            }
        }
        return false;
    }
}