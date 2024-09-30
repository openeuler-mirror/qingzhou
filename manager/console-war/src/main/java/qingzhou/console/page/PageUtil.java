package qingzhou.console.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.view.ViewManager;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.registry.AppInfo;
import qingzhou.registry.GroupInfo;
import qingzhou.registry.MenuInfo;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;
import qingzhou.registry.Registry;

public class PageUtil {
    public static final GroupInfo OTHER_GROUP = new GroupInfo("OTHERS", new String[]{"其他", "en:Other"});

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

    public static String styleFieldValue(String value, ModelFieldInfo fieldInfo) {
        String[] colorInfo = fieldInfo.getColor();
        if (colorInfo == null) return value;

        for (String condition : colorInfo) {
            String[] array = condition.split(":");
            if (array.length != 2) {
                continue;
            }
            if (array[0].equals(value)) {
                return "<label class=\"label transformer label-" + array[1] + "\">" + value + "</label>";
            }
        }

        return value;
    }

    public static String buildMenu(HttpServletRequest request, HttpServletResponse response, Request qzRequest) {
        AppInfo appInfo = SystemController.getAppInfo(qzRequest.getApp());

        MenuInfo[] rootMenuInfos = appInfo.getMenuInfos();
        MenuItem[] rootMenuItems = new MenuItem[rootMenuInfos.length];
        for (int i = 0; i < rootMenuInfos.length; i++) {
            rootMenuItems[i] = createMenuItem(rootMenuInfos[i]);
        }

        List<ModelInfo> noMenuModels = new ArrayList<>();
        for (ModelInfo modelInfo : appInfo.getModelInfos()) {
            if (modelInfo.isHidden()) continue;
            String menu = modelInfo.getMenu();
            MenuItem modelParentMenu = findMenuItem(menu, rootMenuItems);
            if (modelParentMenu != null) {
                modelParentMenu.addModelInfo(modelInfo);
            } else {
                noMenuModels.add(modelInfo);
            }
        }

        StringBuilder menuHtml = new StringBuilder();
        for (ModelInfo noMenuModel : noMenuModels) {
            menuHtml.append(buildModelMenu(noMenuModel, qzRequest, request, response));
        }
        for (MenuItem rootMenu : rootMenuItems) {
            menuHtml.append(buildParentMenu(0, rootMenu, qzRequest, request, response));
        }
        return menuHtml.toString();
    }

    private static MenuItem findMenuItem(String name, MenuItem[] menuItems) {
        for (MenuItem menuItem : menuItems) {
            if (menuItem.getName().equals(name)) {
                return menuItem;
            }
            MenuItem found = findMenuItem(name, menuItem.getChildren().toArray(new MenuItem[0]));
            if (found != null) return found;
        }
        return null;
    }

    private static MenuItem createMenuItem(MenuInfo menuInfo) {
        MenuItem menuItem = new MenuItem();
        menuItem.setName(menuInfo.getName());
        menuItem.setIcon(menuInfo.getIcon());
        menuItem.setI18n(menuInfo.getI18n());
        for (MenuInfo child : menuInfo.getChildren()) {
            MenuItem childMenuItem = createMenuItem(child);
            menuItem.getChildren().add(childMenuItem);
        }
        return menuItem;
    }

    private static String buildParentMenu(int paddingLeft, MenuItem menuItem, Request qzRequest, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder menuHtml = new StringBuilder();
        boolean isDefaultActive = "Service".equals(menuItem.getName());
        menuHtml.append("<li class=\"treeview").append(isDefaultActive ? " menu-open expandsub" : "").append("\">");
        menuHtml.append("   <a href=\"javascript:void(0);\">");
        menuHtml.append("       <i class=\"icon icon-").append(menuItem.getIcon()).append("\"></i>");
        menuHtml.append("       <span>").append(I18n.getStringI18n(menuItem.getI18n())).append("</span>");
        menuHtml.append("       <span class=\"pull-right-container\"><i class=\"icon icon-angle-down\"></i></span>");
        menuHtml.append("   </a>");

        boolean modelBuild = false;
        if (!menuItem.getModelInfos().isEmpty()) {
            menuHtml.append("<ul class=\"treeview-menu\" style=\"padding-left: ").append(paddingLeft).append("px;\">");
            menuItem.getModelInfos().forEach(subModel -> menuHtml.append(buildModelMenu(subModel, qzRequest, request, response)));
            modelBuild = true;
        }

        if (!menuItem.getChildren().isEmpty()) {
            if (!modelBuild) {
                menuHtml.append("<ul class=\"treeview-menu\" style=\"padding-left: ").append(paddingLeft).append("px;\">");
            }
            menuItem.getChildren().forEach(subMenu -> menuHtml.append(buildParentMenu(paddingLeft + 25, subMenu, qzRequest, request, response)));
            menuHtml.append("</ul>");
        } else {
            if (modelBuild) {
                menuHtml.append("</ul>");
            }
        }


        menuHtml.append("</li>");
        return menuHtml.toString();
    }

    private static String buildModelMenu(ModelInfo modelInfo, Request qzRequest, HttpServletRequest request, HttpServletResponse response) {
        StringBuilder menuHtml = new StringBuilder();
        menuHtml.append("<li class=\"treeview ").append("\">");
        String contextPath = request.getContextPath();
        String url = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath + DeployerConstants.REST_PREFIX + "/" + ViewManager.htmlView + "/" + qzRequest.getApp() + "/" + modelInfo.getCode() + "/" + modelInfo.getEntrance();
        menuHtml.append("<a href='").append(RESTController.encodeURL(response, url)).append("' modelName='").append(modelInfo.getCode()).append("'>");
        menuHtml.append("<i class='icon icon-").append(modelInfo.getIcon()).append("'></i>");
        menuHtml.append("<span>").append(I18n.getModelI18n(qzRequest.getApp(), "model." + modelInfo.getCode())).append("</span>");
        menuHtml.append("</a>");
        menuHtml.append("</li>");
        return menuHtml.toString();
    }
}
