package qingzhou.console.page;

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
import qingzhou.registry.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 为前端提供展示需要的数据，应包括：master 菜单数据、app菜单数据、表单 group 分组、国际化等
 * 目前是给 jsp 使用，后续可复用给前后端分离的 html 网页
 * 建议多使用 VO 类的对象，可便于后续转换为 json
 */
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

    private static void printParentMenu(MenuItem menu, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        boolean isDefaultActive = "Service".equals(menu.getMenuName());
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

    private static void printChildrenMenu(MenuItem menu, HttpServletRequest request, HttpServletResponse response, String viewName, Request qzRequest, StringBuilder menuBuilder) {
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
    private static MenuInfo findMenu(String menuName, MenuInfo parent, List<MenuInfo> menuList) {
        // 查找是否已经存在该菜单项
        for (MenuInfo menuData : menuList) {
            if (menuData.getName().equals(menuName)) {
                return menuData; // 已经存在，直接返回
            }

        }
        // 如果不存在，创建新的菜单项
        return null;
    }
    private static MenuItem CreateMenuItem(String menuName, MenuItem parent, List<MenuItem> menuList,MenuInfo info) {
        // 查找是否已经存在该菜单项
        for (MenuItem menuData : menuList) {
            if (menuData.getMenuName().equals(menuName)) {
                return menuData; // 已经存在，直接返回
            }

        }

        // 如果不存在，创建新的菜单项
        MenuItem newMenu = new MenuItem();
        newMenu.setMenuName(menuName);
        newMenu.setMenuIcon(info.getIcon());
        newMenu.setI18ns(info.getI18n());
        newMenu.setOrder(info.getOrder());
        if (parent != null) {
            parent.getChildren().add(newMenu); // 将新创建的菜单项添加到父菜单的children中
        } else {
            menuList.add(newMenu); // 如果是顶级菜单，直接添加到菜单列表
        }

        return newMenu; // 返回新创建的菜单项
    }
    private static void Menusort(List<MenuItem> menuList) {
        menuList.sort(Comparator.comparingInt(MenuItem::getOrder));
        for (MenuItem menuItem : menuList) {
            if (!menuItem.getChildren().isEmpty()){
                Menusort(menuItem.getChildren());
            }
        }

    }
    public static List<MenuItem> getAppMenuList(Request request) {
        List<MenuItem> menus = new ArrayList<>();
        AppInfo appInfo = SystemController.getAppInfo(SystemController.getAppName(request));
        if (appInfo == null) {
            return menus;
        }

        // 分组：按菜单路径的第一级（最高级父菜单）进行分组
        Map<String, List<ModelInfo>> groupMap = appInfo.getModelInfos().stream()
                .filter(modelInfo -> !modelInfo.isHidden())
                .collect(Collectors.groupingBy(modelInfo -> {
                    String[] menuParts = modelInfo.getMenu().split("/");
                    return menuParts[0]; // 第一级菜单（最高级父菜单）
                }));

        groupMap.forEach((menuGroup, models) -> {
            MenuInfo menuData = appInfo.getMenuInfo(menuGroup);
            MenuItem topMenu = new MenuItem();
            if (menuData != null) {
                topMenu.setMenuName(menuGroup);
                topMenu.setMenuIcon(menuData.getIcon());
                topMenu.setI18ns(menuData.getI18n());
                topMenu.setOrder(menuData.getOrder());
            }

            // 对当前组中的 model 进行排序
            models.sort(Comparator.comparingInt(ModelInfo::getOrder));

            models.forEach(modelInfo -> {
                // 分解路径，逐级构建菜单
                String[] menuParts = modelInfo.getMenu().split("/");

                // 每次都从顶级菜单开始
                MenuItem currentMenu = topMenu;
                MenuInfo currentMenuData = menuData;

                // 遍历 menuParts，逐级查找或创建菜单
                for (int i = 1; i < menuParts.length; i++) {
                    currentMenuData = findMenu(menuParts[i], currentMenuData, currentMenuData.getChildren());
                    currentMenu = CreateMenuItem(menuParts[i],currentMenu,currentMenu.getChildren(),currentMenuData);
                }

                // 最终的子菜单
                MenuItem subMenu = new MenuItem();
                subMenu.setMenuName(modelInfo.getCode());
                subMenu.setMenuIcon(modelInfo.getIcon());
                subMenu.setI18ns(modelInfo.getName());
                subMenu.setMenuAction(modelInfo.getEntrance());
                subMenu.setOrder(modelInfo.getOrder());

                // 将子菜单添加到正确的父菜单中
                if (menuData != null) {
                    currentMenu.getChildren().add(subMenu);
                } else {
                    menus.add(subMenu);
                }
            });

            // 将顶级菜单（及其子菜单）添加到菜单列表中
            if (menuData != null) {
                menus.add(topMenu);
            }
        });

        // 对各级菜单进行排序
        Menusort(menus);

        return menus;
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
}
