package qingzhou.console.page;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.ActionType;
import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.rest.SecurityController;
import qingzhou.console.view.type.HtmlView;
import qingzhou.console.view.type.JsonView;
import qingzhou.core.DeployerConstants;
import qingzhou.core.ItemData;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.registry.*;
import qingzhou.engine.util.Utils;

public class PageUtil {
    private static Boolean standaloneMode;
    public static final ItemData OTHER_GROUP = new ItemData("OTHERS", new String[]{"其他", "en:Other"});

    public static boolean getStandaloneMode() {
        if (standaloneMode == null) {
            Map<String, String> config = (Map<String, String>) ((Map<String, Object>) SystemController.getModuleContext().getConfig()).get("deployer");
            standaloneMode = config != null && Boolean.parseBoolean(config.get("standalone")); // 单应用模式 == tw8.0模式
        }
        return standaloneMode;
    }

    public static String getPlaceholder(ModelFieldInfo modelField, String qzApp, String qzModel, boolean isForm) {
        String placeholder = modelField.getPlaceholder();

        if (isForm && modelField.isShowLabel()) {
            return placeholder;
        }

        String i18nPlaceholder = I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + modelField.getCode());
        return Utils.notBlank(placeholder) ? placeholder : i18nPlaceholder;
    }

    public static Map<String, List<String>> groupedFields(Collection<String> fieldNames, ModelInfo modelInfo) {
        Map<String, List<String>> groupedFields = new LinkedHashMap<>();
        List<String> defaultGroup = new LinkedList<>();
        for (String fieldName : fieldNames) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField == null)
                continue; // 用户模块没有 enableOtp 字段，但配置的数据是有的，这里会为 null
            String group = modelField.getGroup();
            if (Utils.isBlank(group)) {
                defaultGroup.add(fieldName);
            } else {
                List<String> fields = groupedFields.computeIfAbsent(group, s -> new LinkedList<>());
                fields.add(fieldName);
            }
        }

        if (!defaultGroup.isEmpty()) {
            groupedFields.put("", defaultGroup);
        }

        return groupedFields;
    }

    public static boolean hasGroup(Map<String, List<String>> groupedFields) {
        if (!groupedFields.isEmpty()) {
            return groupedFields.size() > 1 || Utils.notBlank(groupedFields.keySet().iterator().next());
        }
        return false;
    }

    public static String[] filterActions(String[] checkActions, String qzApp, String qzModel,
                                         HttpServletRequest request) {
        List<String> filteredActions = new ArrayList<>();
        for (String action : checkActions) {
            if (SecurityController.isActionPermitted(qzApp, qzModel, action, request)) {
                filteredActions.add(action);
            }
        }
        return filteredActions.toArray(new String[0]);
    }

    public static String getAppToShow() {
        List<String> allApp = SystemController.getService(Deployer.class).getLocalApps();
        for (String s : allApp) {
            if (!s.equals(DeployerConstants.APP_MASTER)) {
                return s;
            }
        }
        List<String> allAppNames = SystemController.getService(Registry.class).getAllAppNames();
        if (!allAppNames.isEmpty()) {
            return allAppNames.get(0);
        }
        return null;
    }

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                         Request request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/"
                + request.getApp() + "/" + request.getModel() + "/" + actionName;
        return RESTController.encodeURL(servletResponse, appendQueryString(servletRequest, url));
    }

    public static String buildModelUrl(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                       String viewName, String appName, String modelName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + appName
                + "/" + modelName;
        return RESTController.encodeURL(servletResponse, appendQueryString(servletRequest, url));
    }

    public static String buildCustomUrl(HttpServletRequest servletRequest, HttpServletResponse response,
                                        Request request, String viewName, String model, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/"
                + request.getApp() + "/" + model + "/" + actionName;
        return RESTController.encodeURL(response, appendQueryString(servletRequest, url));
    }

    private static String appendQueryString(HttpServletRequest servletRequest, String url) {// 此方法只能给buildRequestUrl、buildCustomUrl这两个拼接url的方法使用
        String queryString = servletRequest.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return url;
        }

        Set<String> seenParamNames = new HashSet<>();
        StringBuilder newQueryString = new StringBuilder();

        String[] params = queryString.split("&");
        for (String param : params) {
            int equalsIndex = param.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }

            String name = param.substring(0, equalsIndex);

            if (seenParamNames.contains(name)) {
                continue;
            }

            seenParamNames.add(name);
            if (!name.startsWith(DeployerConstants.SUB_MENU_PARAMETER_FLAG)) {
                continue;
            }

            if (newQueryString.length() > 0) {
                newQueryString.append('&');
            }
            newQueryString.append(param);
        }

        if (newQueryString.length() > 0) {
            url += (url.contains("?") ? "&" : "?") + newQueryString;
        }

        return url;
    }

    public static String styleFieldValue(String value, RequestImpl qzRequest, ModelFieldInfo fieldInfo) {
        String styleValue = getInputTypeStyle(value, qzRequest, fieldInfo);
        if (Utils.isBlank(styleValue))
            return styleValue;

        try {
            String[] colorInfo = fieldInfo.getColor();
            if (colorInfo == null)
                return styleValue;

            String colorStyle = "";
            for (String condition : colorInfo) {
                String[] array = condition.split(":");
                if (array.length != 2) {
                    continue;
                }
                if (array[0].equals(value)) {
                    colorStyle = "color:" + array[1];
                }
            }
            int ignore = fieldInfo.getIgnore();
            if (ignore > 0 && styleValue.length() > ignore) {
                value = value.replaceAll("\"", "&quot;");
                return "<span style=\" " + colorStyle + " \" data-toggle=\"tooltip\" title=\"" + value + "\">"
                        + styleValue.substring(0, ignore) + "...</span>";
            } else {
                if (Utils.notBlank(colorStyle)) {
                    return "<span style=\"" + colorStyle + "\">" + styleValue + "</span>";
                }
            }

            return styleValue;
        } catch (Exception e) {
            return styleValue;
        }
    }

    public static String getInputTypeStyle(String value, RequestImpl qzRequest, ModelFieldInfo fieldInfo) {
        if (Utils.isBlank(value))
            return value;

        InputType inputType = fieldInfo.getInputType();
        switch (inputType) {
            case markdown:
                value = "<div class=\"markdownview\">" + value + "</div>";
                break;
            case datetime:
                value = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT).format(parseDate(value));
                break;
            case range_datetime:
                String sp = fieldInfo.getSeparator();
                String[] dates = value.split(sp);
                SimpleDateFormat format = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT);
                value = format.format(parseDate(dates[0])) + sp + format.format(parseDate(dates[1]));
                break;
            case radio:
            case select:
                for (ItemData itemData : SystemController.getOptions(qzRequest, fieldInfo.getCode())) {
                    String option = itemData.getName();
                    String optionI18n = I18n.getStringI18n(itemData.getI18n());
                    if (Objects.equals(value, option)) {
                        value = optionI18n;
                        break;
                    }
                }
                break;
            case multiselect:
            case checkbox:
            case sortable_checkbox:
                String[] split = value.split(fieldInfo.getSeparator());
                List<String> list = new LinkedList<>();

                Map<String, String> optionI18nMap = new HashMap<>();// 这里将i18n的数据提前整理好，否则会影响排序
                for (ItemData itemData : SystemController.getOptions(qzRequest, fieldInfo.getCode())) {
                    optionI18nMap.put(itemData.getName(), I18n.getStringI18n(itemData.getI18n()));
                }

                for (String option : split) {
                    String optionI18n = optionI18nMap.get(option);
                    if (optionI18n != null) {
                        list.add(optionI18n);
                    }
                }

                value = String.join(fieldInfo.getSeparator(), list);
                break;
        }
        return value;
    }

    private static Date parseDate(String value) {
        Date date = new Date();
        date.setTime(Long.parseLong(value));
        return date;
    }

    public static String buildMenu(HttpServletRequest request, HttpServletResponse response, RequestImpl qzRequest) {
        AppInfo appInfo = SystemController.getAppInfo(qzRequest.getApp());
        ModelActionInfo actionInfo = qzRequest.getCachedModelInfo().getModelActionInfo(qzRequest.getAction());
        String modelMenuParameter = null;

        Set<String> showSubMenus = null;
        if (actionInfo.getActionType() == ActionType.sub_menu) {
            String[] menuModels = actionInfo.getSubMenuModels();
            if (menuModels != null && menuModels.length > 0) {
                showSubMenus = new HashSet<>(Arrays.asList(menuModels));
                modelMenuParameter = (DeployerConstants.SUB_MENU_PARAMETER_FLAG + qzRequest.getModel() + "."
                        + qzRequest.getAction() + "=" + qzRequest.getId());
            }
        }

        MenuItem rootMenu = new MenuItem();
        Stream<MenuInfo> temp = Arrays.stream(appInfo.getMenuInfos());
        temp.forEach(menuInfo -> {
            MenuItem menuItem = new MenuItem();
            menuItem.setName(menuInfo.getName());
            menuItem.setIcon(menuInfo.getIcon());
            menuItem.setI18n(menuInfo.getI18n());
            menuItem.setOrder(menuInfo.getOrder());
            menuItem.setModel(menuInfo.getModel());
            menuItem.setAction(menuInfo.getAction());
            String parent = menuInfo.getParent();
            MenuItem foundParent = rootMenu.findMenu(parent);
            if (foundParent != null) {
                foundParent.addMenuItem(menuItem);
            } else {
                rootMenu.addMenuItem(menuItem);
            }
        });

        // 将 Model 菜单挂到 导航 菜单上
        ModelMenu:
        for (ModelInfo modelInfo : appInfo.getModelInfos()) {
            if (showSubMenus != null) { // 是否使用 子菜单 功能
                if (!showSubMenus.contains(modelInfo.getCode())) { // 是否在子菜单范围内
                    continue;
                }
            } else {
                // 未使用子菜单，需要判断是否 hidden
                if (modelInfo.isHidden())
                    continue;

                if (appInfo.getName().equals(DeployerConstants.APP_MASTER)) {
                    if (modelInfo.getCode().equals(DeployerConstants.MODEL_APP)) {
                        if (getStandaloneMode())
                            continue;
                    }

                    if (SystemController.getService(Deployer.class).getAuthAdapter() != null) {
                        String[] forbiddenModels = new String[]{
                                DeployerConstants.MODEL_USER,
                                DeployerConstants.MODEL_ROLE
                        };
                        for (String forbiddenModel : forbiddenModels) {
                            if (modelInfo.getCode().equals(forbiddenModel))
                                continue ModelMenu;
                        }
                    }
                }
            }

            boolean actionPermitted = SecurityController.isActionPermitted(appInfo.getName(), modelInfo.getCode(),
                    modelInfo.getEntrance(), request);
            if (!actionPermitted)
                continue;

            String menu = modelInfo.getMenu();
            MenuItem foundParent = rootMenu.findMenu(menu);
            if (foundParent != null) {
                foundParent.addModelInfo(modelInfo);
            } else {
                rootMenu.addModelInfo(modelInfo);
            }
        }

        StringBuilder menuHtml = new StringBuilder();
        // 同一级别，Model 菜单排前面
        for (ModelInfo noMenuModel : rootMenu.getSubModelList()) {
            menuHtml.append(buildModelMenu(0, noMenuModel, qzRequest, request, response, modelMenuParameter));
        }
        // 同一级别，导航 菜单排后面
        for (MenuItem levelOneMenu : rootMenu.getSubMenuList()) {
            menuHtml.append(buildParentMenu(0, levelOneMenu, qzRequest, request, response, modelMenuParameter));
        }
        return menuHtml.toString();
    }

    private static boolean isEmptyMenu(MenuItem menuItem) {
        if (!menuItem.getSubModelList().isEmpty())
            return false;

        for (MenuItem item : menuItem.getSubMenuList()) {
            if (!isEmptyMenu(item))
                return false;
        }

        return true;
    }

    private static String buildParentMenu(int level, MenuItem menuItem, Request qzRequest, HttpServletRequest request,
                                          HttpServletResponse response, String modelMenuParameter) {
        // 空菜单不显示
        if (isEmptyMenu(menuItem)) {
            return "";
        }

        StringBuilder menuHtml = new StringBuilder();

        // qingzhou.app.master.Main.XXX
        boolean isDefaultActive = "Business".equals(menuItem.getName());
        menuHtml.append("<li class=\"treeview").append(isDefaultActive ? " menu-open expandsub" : "").append("\">");
        if (Utils.notBlank(menuItem.getModel()) && Utils.notBlank(menuItem.getAction())) {
            // 菜单添加action
            String contextPath = request.getContextPath();
            String actionUrl = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1)
                    : contextPath + DeployerConstants.REST_PREFIX + "/" + JsonView.FLAG + "/" + qzRequest.getApp() + "/"
                    + menuItem.getModel() + "/" + menuItem.getAction();
            if (Utils.notBlank(modelMenuParameter)) {
                actionUrl += "?" + modelMenuParameter;
            }
            menuHtml.append("   <a href=\"javascript:void(0);\"  action=\"" + actionUrl + "\" style=\"text-indent:")
                    .append(level).append("px;\">");
        } else {
            menuHtml.append("   <a href=\"javascript:void(0);\" style=\"text-indent:").append(level).append("px;\">");
        }
        menuHtml.append("       <i class=\"icon icon-").append(menuItem.getIcon()).append("\"></i>");
        menuHtml.append("       <span>").append(I18n.getStringI18n(menuItem.getI18n())).append("</span>");
        menuHtml.append("       <span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
        menuHtml.append("   </a>");

        int menuTextLeft = level * 10;

        boolean menuBegan = false;
        if (!menuItem.getSubModelList().isEmpty()) {
            menuHtml.append("<ul class=\"treeview-menu\">");
            menuBegan = true;

            menuItem.getSubModelList().forEach(subModel -> menuHtml
                    .append(buildModelMenu(menuTextLeft, subModel, qzRequest, request, response, modelMenuParameter)));
        }

        if (!menuItem.getSubMenuList().isEmpty()) {
            if (!menuBegan) {
                menuHtml.append("<ul class=\"treeview-menu\">");
                menuBegan = true;
            }

            menuItem.getSubMenuList().forEach(subMenu -> menuHtml
                    .append(buildParentMenu(level + 1, subMenu, qzRequest, request, response, modelMenuParameter)));
        }

        if (menuBegan) {
            menuHtml.append("</ul>");
        }

        menuHtml.append("</li>");
        return menuHtml.toString();
    }

    private static String buildModelMenu(int menuTextLeft, ModelInfo modelInfo, Request qzRequest,
                                         HttpServletRequest request, HttpServletResponse response, String urlParameter) {
        String app = qzRequest.getApp();
        String model = modelInfo.getCode();
        String action = modelInfo.getEntrance();

        StringBuilder menuHtml = new StringBuilder();
        menuHtml.append("<li class=\"treeview ").append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1)
                : contextPath + DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + qzRequest.getApp() + "/"
                + model + "/" + action;
        if (Utils.notBlank(urlParameter)) {
            url += "?" + urlParameter;
        }
        menuHtml.append("<a href='").append(RESTController.encodeURL(response, url)).append("' modelName='")
                .append(model).append("'").append("style=\" text-indent:").append(menuTextLeft).append("px;\"")
                .append(">");
        menuHtml.append("<i class='icon icon-").append(modelInfo.getIcon()).append("'></i>");
        menuHtml.append("<span>").append(I18n.getModelI18n(app, "model." + model)).append("</span>");
        menuHtml.append("</a>");
        menuHtml.append("</li>");
        return menuHtml.toString();
    }

    public static void groupMultiselectOptions(
            LinkedHashMap<String, String> parentGroupDescriptions,
            LinkedHashMap<String, LinkedHashMap<String, String>> groupedOptions,
            ItemData[] multiselectOptions) {

        // 存储父组的键
        Set<String> parentGroups = new HashSet<>();

        // 处理父组关系
        for (ItemData entry : multiselectOptions) {
            String key = entry.getName();

            // 查找是否有以当前key为前缀的子项，若有则标记当前key为父组
            for (ItemData subEntry : multiselectOptions) {
                String subKey = subEntry.getName();
                if (!subKey.equals(key) && subKey.startsWith(key + DeployerConstants.MULTISELECT_GROUP_SEPARATOR)) {
                    parentGroups.add(key);
                    break; // 跳出内层循环，避免重复计算
                }
            }
        }

        // 处理每个选项，将其按组分类
        for (ItemData entry : multiselectOptions) {
            String value = entry.getName();
            // 将父组选项存储到parentGroupDescriptions中
            if (parentGroups.contains(value)) {
                parentGroupDescriptions.put(value, I18n.getStringI18n(entry.getI18n()));
            } else {
                // 提取组名并将选项存入对应的组
                int separatorIndex = value.lastIndexOf(DeployerConstants.MULTISELECT_GROUP_SEPARATOR);
                String groupName;
                if (separatorIndex != -1) {
                    groupName = value.substring(0, separatorIndex);
                } else {
                    groupName = "";
                }

                // 使用computeIfAbsent确保groupName对应的组存在
                LinkedHashMap<String, String> groupItems = groupedOptions.computeIfAbsent(groupName,
                        k -> new LinkedHashMap<>());
                groupItems.put(value, I18n.getStringI18n(entry.getI18n()));
            }
        }
    }

    public static String getColorStyle(ModelInfo modelInfo, String fieldName, String value) {
        ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(fieldName);
        String[] color = modelFieldInfo.getColor();
        if (color != null) {
            for (String condition : color) {
                String[] array = condition.split(":");
                if (array.length != 2) {
                    continue;
                }
                if (array[0].equals(value)) {
                    return "color:" + array[1];
                }
            }
        }
        return "";
    }

    public static Map<String, String> stringToMap(String str, String SP) {
        Map<String, String> map = new LinkedHashMap<>();
        if (Utils.isBlank(str)) {
            return map;
        }
        String[] envArr = str.split(SP);
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

    public static int getIndex(Object[] objects, Object object) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].equals(object)) {
                return i;
            }
        }
        throw new IllegalArgumentException("not found");
    }
}
