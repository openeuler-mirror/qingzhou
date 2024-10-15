package qingzhou.console.page;

import qingzhou.api.FieldType;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.view.type.HtmlView;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.registry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class PageUtil {
    public static final ItemInfo OTHER_GROUP = new ItemInfo("OTHERS", new String[]{"其他", "en:Other"});

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

    public static String buildRequestUrl(HttpServletRequest servletRequest, HttpServletResponse response, Request request, String viewName, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + request.getApp() + "/" + request.getModel() + "/" + actionName;
        return response.encodeURL(url);
    }

    public static String buildCustomUrl(HttpServletRequest servletRequest, HttpServletResponse response, Request request, String viewName, String model, String actionName) {
        String url = servletRequest.getContextPath() + DeployerConstants.REST_PREFIX + "/" + viewName + "/" + request.getApp() + "/" + model + "/" + actionName;
        return response.encodeURL(url);
    }

    public static ModelActionInfo[] listBachActions(Request request, Response response, String loginUser) {
        List<ModelActionInfo> actions = new ArrayList<>();

        final ModelInfo modelInfo = ((RequestImpl) request).getCachedModelInfo();
        for (String actionName : modelInfo.getBatchActionNames()) {
            boolean isShow = false;
            for (Map<String, String> data : response.getDataList()) {
                if (SecurityController.isActionShow(request.getApp(), request.getModel(), actionName, data, loginUser)) {
                    isShow = true;
                    break;
                }
            }
            if (isShow) {
                ModelActionInfo action = modelInfo.getModelActionInfo(actionName);
                actions.add(action);
            }
        }

        actions.sort(Comparator.comparingInt(ModelActionInfo::getOrder));
        return actions.toArray(new ModelActionInfo[0]);
    }

    public static String styleFieldValue(String value, ModelFieldInfo fieldInfo, String qzApp, ModelInfo modelInfo) {
        if (Utils.isBlank(value)) return value;

        try {
            value = getDisplayValue(value, qzApp, modelInfo, fieldInfo);

            String[] colorInfo = fieldInfo.getColor();
            if (colorInfo == null) return value;

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
            int maxLenToShow = fieldInfo.getShowLength();
            if (value.length() > maxLenToShow) {
                return "<span style=\" " + colorStyle + " \" data-toggle=\"tooltip\" title=\"" + value + "\">" + value.substring(0, maxLenToShow) + "...</span>";
            } else {
                if (Utils.notBlank(colorStyle)) {
                    return "<span style=\"" + colorStyle + "\">" + value + "</span>";
                }
            }

            return value;
        } catch (Exception e) {
            return value;
        }
    }

    public static String getDisplayValue(String value, String qzApp, ModelInfo modelInfo, ModelFieldInfo fieldInfo) {
        FieldType fieldType = FieldType.valueOf(fieldInfo.getType());
        switch (fieldType) {
            case markdown:
                value = "<div class=\"markdownview\">" + value + "</div>";
                break;
            case datetime:
                Date date = new Date();
                date.setTime(Long.parseLong(value));
                value = new SimpleDateFormat(DeployerConstants.FIELD_DATETIME_FORMAT).format(date);
                break;
            case radio:
            case select:
                for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldInfo.getCode())) {
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
            case sortablecheckbox:
                String[] split = value.split(fieldInfo.getSeparator());
                List<String> list = new ArrayList<>(split.length);
                for (ItemInfo itemInfo : SystemController.getOptions(qzApp, modelInfo, fieldInfo.getCode())) {
                    String option = itemInfo.getName();
                    String optionI18n = I18n.getStringI18n(itemInfo.getI18n());
                    if (Utils.contains(split, option)) {
                        list.add(optionI18n);
                    }
                }
                value = String.join(fieldInfo.getSeparator(), list);
                break;
        }
        return value;
    }

    public static String buildMenu(HttpServletRequest request, HttpServletResponse response, Request qzRequest) {
        AppInfo appInfo = SystemController.getAppInfo(qzRequest.getApp());

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
            if (modelInfo.isHidden()) continue;
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
            menuHtml.append(buildModelMenu(0, noMenuModel, qzRequest, request, response));
        }
        // 同一级别，导航 菜单排后面
        for (MenuItem levelOneMenu : rootMenu.getSubMenuList()) {
            menuHtml.append(buildParentMenu(0, levelOneMenu, qzRequest, request, response));
        }
        return menuHtml.toString();
    }

    private static String buildParentMenu(int level, MenuItem menuItem, Request qzRequest, HttpServletRequest request, HttpServletResponse response) {
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

            menuItem.getSubModelList().forEach(subModel -> menuHtml.append(buildModelMenu(menuTextLeft, subModel, qzRequest, request, response)));
        }

        if (!menuItem.getSubMenuList().isEmpty()) {
            if (!menuBegan) {
                menuHtml.append("<ul class=\"treeview-menu\">");
                menuBegan = true;
            }

            menuItem.getSubMenuList().forEach(subMenu -> menuHtml.append(buildParentMenu(level + 1, subMenu, qzRequest, request, response)));
        }

        if (menuBegan) {
            menuHtml.append("</ul>");
        }

        menuHtml.append("</li>");
        return menuHtml.toString();
    }

    private static String buildModelMenu(int menuTextLeft, ModelInfo modelInfo, Request qzRequest, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder menuHtml = new StringBuilder();
        menuHtml.append("<li class=\"treeview ").append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + DeployerConstants.REST_PREFIX + "/" + HtmlView.FLAG + "/" + qzRequest.getApp() + "/" + modelInfo.getCode() + "/" + modelInfo.getEntrance();
        menuHtml.append("<a href='").append(RESTController.encodeURL(response, url)).append("' modelName='").append(modelInfo.getCode()).append("'").append("style=\" text-indent:").append(menuTextLeft).append("px;\"").append(">");
        menuHtml.append("<i class='icon icon-").append(modelInfo.getIcon()).append("'></i>");
        menuHtml.append("<span>").append(I18n.getModelI18n(qzRequest.getApp(), "model." + modelInfo.getCode())).append("</span>");
        menuHtml.append("</a>");
        menuHtml.append("</li>");
        return menuHtml.toString();
    }
}
