package qingzhou.console.page;

import qingzhou.console.AppStub;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.I18n;
import qingzhou.console.Validator;
import qingzhou.console.controller.rest.AccessControl;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyManager;
import qingzhou.framework.api.*;
import qingzhou.framework.console.ConsoleI18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.pattern.Visitor;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 为前端提供展示需要的数据，应包括：master 菜单数据、app菜单数据、表单 group 分组、国际化等
 * 目前是给 jsp 使用，后续可复用给前后端分离的 html 网页
 * 建议多使用 VO 类的对象，可便于后续转换为 json
 */
public class PageBackendService {

    private static final String DEFAULT_EXPAND_MENU_GROUP_NAME = "Service";
    public static String TARGET_TYPE_SET_FLAG = "targetType";
    public static String TARGET_NAME_SET_FLAG = "targetName";

    private PageBackendService() {
    }

    public static String getMasterAppI18NString(String key, Lang lang) {
        return ConsoleI18n.getI18N(lang, key);
    }

    public static String getMasterAppI18NString(String key) {
        return ConsoleI18n.getI18N(I18n.getI18nLang(), key);
    }

    public static ModelManager getModelManager(String appName) {
        return AppStub.getConsoleContext(appName).getModelManager();
    }

    static void printParentMenu(Properties menu, String appName, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        String model = menu.getProperty("name");
        String menuText = "未分类";
        boolean isDefaultActive = false;
        if (StringUtil.notBlank(model)) {
            MenuInfo menuInfo = AppStub.getConsoleContext(appName).getMenuInfo(model);
            if (menuInfo == null) {
                // todo
                //menuInfo = ((ConsoleContextImpl) Main.getInternalService(ConsoleContextFinder.class).find(Constants.QINGZHOU_DEFAULT_APP_NAME)).getMenuInfo(model);
            } else {
                isDefaultActive = DEFAULT_EXPAND_MENU_GROUP_NAME.equals(menuInfo.getMenuName());
                menuText = I18n.getString(menuInfo.getMenuI18n());
            }
        }
        menuBuilder.append("<li class=\"treeview").append(isDefaultActive ? " menu-open expandsub" : "").append(model.equals(curModel) ? " active" : "").append("\">");
        menuBuilder.append("<a href=\"javascript:void(0);\">");
        menuBuilder.append(" <i class=\"icon icon-").append(menu.getProperty("icon")).append("\"></i>");
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

    static void printChildrenMenu(Properties menu, HttpServletRequest request, HttpServletResponse response, String viewName, String appName, String curModel, StringBuilder menuBuilder) {
        String model = menu.getProperty("name");
        String action = menu.getProperty("entryAction");
        menuBuilder.append("<li class=\"treeview ").append((model.equals(curModel) ? " active" : "")).append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + RESTController.REST_PREFIX + "/" + viewName + "/" + appName + "/" + model + "/" + action;
        menuBuilder.append("<a href='").append(encodeURL(request, response, url)).append("' modelName='").append(model).append("'>");
        menuBuilder.append("<i class='icon icon-").append(menu.getProperty("icon")).append("'></i>");
        menuBuilder.append("<span>").append(I18n.getString(appName, "model." + model)).append("</span>");
        menuBuilder.append("</a>");
        menuBuilder.append("</li>");
    }

    public static String buildMenuHtmlBuilder(List<Properties> models, String loginUser, HttpServletRequest request, HttpServletResponse response, String viewName, String appName, String curModel) {
        StringBuilder builder = new StringBuilder();
        buildMenuHtmlBuilder(models, request, response, viewName, appName, curModel, builder, true);
        String menus = builder.toString();
        return String.format(menus, " ");
    }

    private static void buildMenuHtmlBuilder(List<Properties> models, HttpServletRequest request, HttpServletResponse response, String viewName, String appName, String curModel, StringBuilder builder, boolean needFavoritesMenu) {
        models.sort(java.util.Comparator.comparing(o -> String.valueOf(o.get("order"))));

        for (int i = 0; i < models.size(); i++) {
            if (needFavoritesMenu && i == 1) {
                builder.append("%s");
            }
            Properties menu = models.get(i);
            StringBuilder childrenBuilder = new StringBuilder();
            Object c = menu.get("children");
            if (c == null) {
                printChildrenMenu(menu, request, response, viewName, appName, curModel, childrenBuilder);
                builder.append(childrenBuilder);
            } else {
                List<Properties> childrenMenus = (List<Properties>) c;
                StringBuilder parentBuilder = new StringBuilder();
                buildMenuHtmlBuilder(childrenMenus, request, response, viewName, appName, curModel, childrenBuilder, false);
                printParentMenu(menu, appName, curModel, parentBuilder, childrenBuilder);
                builder.append(parentBuilder.toString());
            }

        }
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
        ConsoleContext consoleContext = AppStub.getConsoleContext(appName);
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
                MenuInfo menuInfo = consoleContext.getMenuInfo(menuName);
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

    public static String encodeURL(HttpServletRequest request, HttpServletResponse response, String url) {// 应该优先考虑使用 非静态 的同名方法，而不是这个
        return response.encodeURL(encodeTarget(request, url));
    }

    public static String encodeTarget(HttpServletRequest request, String url) {
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

    public static void multiSelectGroup(LinkedHashMap<String, String> groupDes,
                                        LinkedHashMap<String, LinkedHashMap<String, String>> groupedMap,
                                        Options optionsManager) {
        LinkedHashMap<String, LinkedHashMap<String, String>> tempGroup = new LinkedHashMap<>();
        LinkedHashMap<String, String> twoGroup = new LinkedHashMap<>();
        for (Option option : optionsManager.options()) {
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
        if (groupedMap.size() > 0) {
            groupDes.putAll(twoGroup);
        } else {
            groupedMap.putAll(tempGroup);
        }
    }

    public static Map<String, Map<String, ModelField>> getGroupedModelFieldMap(Request request) {
        Map<String, Map<String, ModelField>> result = new LinkedHashMap<>();
        ModelManager manager = ConsoleWarHelper.getAppManager().getAppInfo(request.getAppName()).getAppContext().getConsoleContext().getModelManager();
        String modelName = request.getModelName();
        for (String groupName : manager.getGroupNames(modelName)) {
            Map<String, ModelField> map = new LinkedHashMap<>();
            String[] fieldNamesByGroup = manager.getFieldNamesByGroup(modelName, groupName);
            for (String f : fieldNamesByGroup) {
                ModelField modelField = manager.getModelField(modelName, f);
                map.put(f, modelField);
            }
            result.put(groupName, map);
        }

        return result;
    }

    public static String decryptWithConsolePrivateKey(String input) {
        if (StringUtil.isBlank(input)) {
            return input;
        }
        try {
            File secureFile = getSecureFile(ConsoleWarHelper.getDomain());
            KeyManager keyManager = ConsoleWarHelper.getCryptoService().getKeyManager();
            String pubKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
            String priKey = keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.privateKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);

            return ConsoleWarHelper.getCryptoService().getPublicKeyCipher(pubKey, priKey).decryptWithPrivateKey(input);
        } catch (Exception e) {
            e.printStackTrace();
            return input;
        }
    }

    public static synchronized File getSecureFile(File domain) throws IOException {
        File secureDir = FileUtil.newFile(domain, "data", "secure");
        FileUtil.mkdirs(secureDir);
        File secureFile = FileUtil.newFile(secureDir, "secure");
        if (!secureFile.exists()) {
            if (!secureFile.createNewFile()) {
                throw ExceptionUtil.unexpectedException(secureFile.getPath());
            }
        }

        return secureFile;
    }

    public static String getPublicKeyString() throws Exception {
        CryptoService cryptoService = ConsoleWarHelper.getCryptoService();
        KeyManager keyManager = cryptoService.getKeyManager();
        File secureFile = getSecureFile(ConsoleWarHelper.getDomain());
        return keyManager.getKeyPairOrElseInit(secureFile, ConsoleConstants.publicKeyName, ConsoleConstants.publicKeyName, ConsoleConstants.privateKeyName, null);
    }

    // jsp加密： 获取非对称算法密钥长度
    public static int getKeySize() { // login.jsp 使用
        return 1024;// 保持一致：PasswordCipherFactory.KeyGenerate
    }

    public static String getSubmitActionName(Request request) {
        boolean isEdit = Objects.equals(EditModel.ACTION_NAME_EDIT, request.getActionName());
        final ModelManager modelManager = ConsoleWarHelper.getAppManager().getAppInfo(request.getAppName()).getAppContext().getConsoleContext().getModelManager();
        if (modelManager == null) {
            return null;
        }
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

    public static String isActionEffective(Request request, Map<String, String> obj, ModelAction modelAction) {
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

    public static void error(String msg, Throwable t) {
        ConsoleWarHelper.getLogger().error(msg, t);
    }

    public static void warn(String msg) {
        ConsoleWarHelper.getLogger().warn(msg);
    }

    public static Map<String, String> modelFieldEffectiveWhenMap(Request request) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        String modelName = request.getModelName();
        for (String fieldName : modelManager.getFieldNames(modelName)) {
            ModelField modelField = modelManager.getModelField(modelName, fieldName);
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
        ModelField modelField = modelManager.getModelField(modelName, allFieldNames[i]);
        FieldType fieldType = modelField.type();
        return fieldType == FieldType.radio || fieldType == FieldType.bool || fieldType == FieldType.select || fieldType == FieldType.groupedMultiselect || fieldType == FieldType.checkbox || fieldType == FieldType.sortableCheckbox;
    }

    public static boolean isFieldReadOnly(Request request, String fieldName) {
        final ModelManager modelManager = getModelManager(request.getAppName());
        if (modelManager == null) {
            return false;
        }
        ModelField modelField = modelManager.getModelField(request.getModelName(), fieldName);
        if (modelField.maxLength() < 1) {
            return true;
        }
        if (modelField.disableOnCreate() && modelField.disableOnEdit()) {
            return true;
        }

        return false;
    }

    public static boolean isAjaxAction(String actionName) {
        return EditModel.ACTION_NAME_UPDATE.equals(actionName) ||
                AddModel.ACTION_NAME_ADD.equals(actionName) ||
                DeleteModel.ACTION_NAME_DELETE.equals(actionName);
    }

    /********************* 批量操作 start ************************/
    public static ModelAction[] listCommonOps(Request request, Response response) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, batchVisitor);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    public static ModelAction[] listModelBaseOps(Request request, Response response, Map<String, String> obj) throws Exception {
        BatchVisitor batchVisitor = new BatchVisitor();
        visitActions(request, response, batchVisitor, new ArrayList<Map<String, String>>() {{
            add(obj);
        }}, true);
        return batchVisitor.modelActions.toArray(new ModelAction[0]);
    }

    //公共操作列表
    public static boolean needOperationColumn(Request request, Response response) throws Exception {
        final boolean[] needOperationColumn = {false};
        visitActions(request, response,
                obj -> {
                    needOperationColumn[0] = true;
                    return false;
                });
        return needOperationColumn[0];
    }

    private static void visitActions(Request request, Response response, Visitor<ModelAction> visitor) throws Exception {
        visitActions(request, response, visitor, response.getDataList(), true);
    }

    private static void visitActions(Request request, Response response, Visitor<ModelAction> visitor, List<Map<String, String>> datas, boolean checkEffective) throws Exception {
        final String appName = request.getAppName();
        final ModelManager modelManager = getModelManager(appName);
        if (modelManager == null) {
            return;
        }
        final String modelName = request.getModelName();
        boolean hasId = hasIDField(request);
        if (hasId && !response.getDataList().isEmpty()) {
            for (String ac : modelManager.getActionNames(modelName)) {
                ModelAction action = modelManager.getModelAction(modelName, ac);
                for (Map<String, String> data : datas) {
                    if (checkEffective && isActionEffective(request, data, action) != null) {
                        continue;
                    }

                    final String actionName = action.name();
                    if (EditModel.ACTION_NAME_EDIT.equals(actionName)) {
                        continue;
                    }

                    if (!action.showToList() || action.showToListHead()) {
                        continue;
                    }

                    if (visitor.visitAndEnd(action)) {
                        break;
                    }
                }
            }
        }
    }

    private static class BatchVisitor implements Visitor<ModelAction> {
        List<ModelAction> modelActions = new ArrayList<>();

        @Override
        public boolean visitAndEnd(ModelAction action) {
            if (!action.supportBatch()) {
                return true;
            }

            if (!modelActions.contains(action)) {
                modelActions.add(action);
            }

            return false;
        }
    }
    /********************* 批量操作 end ************************/
}
