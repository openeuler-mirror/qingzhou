package qingzhou.console.page;

import qingzhou.api.ActionType;
import qingzhou.api.InputType;
import qingzhou.api.Request;
import qingzhou.core.*;
import qingzhou.core.Deployer;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.view.type.HtmlView;
import qingzhou.engine.util.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class PageUtil {
    public static final ItemInfo OTHER_GROUP = new ItemInfo("OTHERS", new String[]{"其他", "en:Other"});

    public static Map<String, List<String>> groupedFields(Collection<String> fieldNames, ModelInfo modelInfo) {
        Map<String, List<String>> groupedFields = new LinkedHashMap<>();
        List<String> defaultGroup = new LinkedList<>();
        for (String fieldName : fieldNames) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField == null) continue; // 用户模块没有 enableOtp 字段，但配置的数据是有的，这里会为 null
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

    public static String[] filterActions(String[] checkActions, String qzApp, String qzModel, String currentUser) {
        List<String> filteredActions = new ArrayList<>();
        for (String action : checkActions) {
            if (SecurityController.isActionPermitted(qzApp, qzModel, action, currentUser)) {
                filteredActions.add(action);
            }
        }
        return filteredActions.toArray(new String[0]);
    }

    public static String getAppToShow() {
        List<String> allApp = SystemController.getService(Deployer.class).getAllApp();
        for (String s : allApp) {
            if (!s.equals(DeployerConstants.APP_SYSTEM)) {
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
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + request.getApp() + "/" + request.getModel() + "/" + actionName;
        return RESTController.encodeURL(servletResponse, appendQueryString(servletRequest, request, url));
    }

    public static String buildCustomUrl(HttpServletRequest servletRequest, HttpServletResponse response,
                                        Request request, String viewName, String model, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + request.getApp() + "/" + model + "/" + actionName;
        return RESTController.encodeURL(response, appendQueryString(servletRequest, request, url));
    }

    private static String appendQueryString(HttpServletRequest servletRequest, Request request, String url) {// 此方法只能给buildRequestUrl、buildCustomUrl这两个拼接url的方法使用
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
        if (Utils.isBlank(styleValue)) return styleValue;

        try {
            String[] colorInfo = fieldInfo.getColor();
            if (colorInfo == null) return styleValue;

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
                return "<span style=\" " + colorStyle + " \" data-toggle=\"tooltip\" title=\"" + value + "\">" + styleValue.substring(0, ignore) + "...</span>";
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
        if (Utils.isBlank(value)) return value;

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
                for (ItemInfo itemInfo : SystemController.getOptions(qzRequest, fieldInfo.getCode())) {
                    String option = itemInfo.getName();
                    String optionI18n = I18n.getStringI18n(itemInfo.getI18n());
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
                for (ItemInfo itemInfo : SystemController.getOptions(qzRequest, fieldInfo.getCode())) {
                    optionI18nMap.put(itemInfo.getName(), I18n.getStringI18n(itemInfo.getI18n()));
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
                modelMenuParameter = (DeployerConstants.SUB_MENU_PARAMETER_FLAG + qzRequest.getModel() + "." + qzRequest.getAction() + "=" + qzRequest.getId());
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

            String parent = menuInfo.getParent();
            MenuItem foundParent = rootMenu.findMenu(parent);
            if (foundParent != null) {
                foundParent.addMenuItem(menuItem);
            } else {
                rootMenu.addMenuItem(menuItem);
            }
        });

        // 将 Model 菜单挂到 导航 菜单上
        for (ModelInfo modelInfo : appInfo.getModelInfos()) {
            if (showSubMenus != null) { // 是否使用 子菜单 功能
                if (!showSubMenus.contains(modelInfo.getCode())) { //  是否在子菜单范围内
                    continue;
                }
            } else {
                // 未使用子菜单，需要判断是否 hidden
                if (modelInfo.isHidden()) continue;
            }

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
        if (!menuItem.getSubModelList().isEmpty()) return false;

        for (MenuItem item : menuItem.getSubMenuList()) {
            if (!isEmptyMenu(item)) return false;
        }

        return true;
    }

    private static String buildParentMenu(int level, MenuItem menuItem, Request qzRequest, HttpServletRequest request, HttpServletResponse response, String modelMenuParameter) {
        // 空菜单不显示
        if (isEmptyMenu(menuItem)) {
            return "";
        }

        StringBuilder menuHtml = new StringBuilder();

        // qingzhou.app.system.Main.XXX
        boolean isDefaultActive = "Business".equals(menuItem.getName());

        menuHtml.append("<li class=\"treeview").append(isDefaultActive ? " menu-open expandsub" : "").append("\">");
        menuHtml.append("   <a href=\"javascript:void(0);\" style=\"text-indent:").append(level).append("px;\">");
        menuHtml.append("       <i class=\"icon icon-").append(menuItem.getIcon()).append("\"></i>");
        menuHtml.append("       <span>").append(I18n.getStringI18n(menuItem.getI18n())).append("</span>");
        menuHtml.append("       <span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
        menuHtml.append("   </a>");

        int menuTextLeft = level * 10;

        boolean menuBegan = false;
        if (!menuItem.getSubModelList().isEmpty()) {
            menuHtml.append("<ul class=\"treeview-menu\">");
            menuBegan = true;

            menuItem.getSubModelList().forEach(subModel -> menuHtml.append(buildModelMenu(menuTextLeft, subModel, qzRequest, request, response, modelMenuParameter)));
        }

        if (!menuItem.getSubMenuList().isEmpty()) {
            if (!menuBegan) {
                menuHtml.append("<ul class=\"treeview-menu\">");
                menuBegan = true;
            }

            menuItem.getSubMenuList().forEach(subMenu -> menuHtml.append(buildParentMenu(level + 1, subMenu, qzRequest, request, response, modelMenuParameter)));
        }

        if (menuBegan) {
            menuHtml.append("</ul>");
        }

        menuHtml.append("</li>");
        return menuHtml.toString();
    }

    private static String buildModelMenu(int menuTextLeft, ModelInfo modelInfo, Request qzRequest, HttpServletRequest request, HttpServletResponse response, String urlParameter) {
        StringBuilder menuHtml = new StringBuilder();
        menuHtml.append("<li class=\"treeview ").append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + qzRequest.getApp() + "/" + modelInfo.getCode() + "/" + modelInfo.getEntrance();
        if (Utils.notBlank(urlParameter)) {
            url += "?" + urlParameter;
        }
        menuHtml.append("<a href='").append(RESTController.encodeURL(response, url)).append("' modelName='").append(modelInfo.getCode()).append("'").append("style=\" text-indent:").append(menuTextLeft).append("px;\"").append(">");
        menuHtml.append("<i class='icon icon-").append(modelInfo.getIcon()).append("'></i>");
        menuHtml.append("<span>").append(I18n.getModelI18n(qzRequest.getApp(), "model." + modelInfo.getCode())).append("</span>");
        menuHtml.append("</a>");
        menuHtml.append("</li>");
        return menuHtml.toString();
    }
}
