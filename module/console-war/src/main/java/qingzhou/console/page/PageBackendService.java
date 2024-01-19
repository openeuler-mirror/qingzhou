package qingzhou.console.page;

import qingzhou.console.controller.rest.AccessControl;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static String getMasterAppI18NString(String key) {
        return I18n.getString(ConsoleConstants.MASTER_APP_NAME, key);
    }

    public static String getMasterAppI18NString(String key, Lang lang) {
        return I18n.getString(ConsoleConstants.MASTER_APP_NAME, key, lang);
    }

    public static ModelManager getModelManager(String appName) {
        return AppStub.getAppConsoleContext(appName).getModelManager();
    }

    static void printParentMenu(Properties menu, String appName, String curModel, StringBuilder menuBuilder, StringBuilder childrenBuilder) {
        String model = menu.getProperty("name");
        String menuText = "未分类";
        boolean isDefaultActive = false;
        if (StringUtil.notBlank(model)) {
            MenuInfo menuInfo = AppStub.getAppConsoleContext(appName).getMenuInfo(model);
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
        ConsoleContext consoleContext = AppStub.getAppConsoleContext(appName);
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
}
