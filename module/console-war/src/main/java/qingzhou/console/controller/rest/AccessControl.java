package qingzhou.console.controller.rest;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.api.AddModel;
import qingzhou.framework.api.DownloadModel;
import qingzhou.framework.api.EditModel;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.ShowModel;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查是否有访问权限
 */
public class AccessControl implements Filter<RestContext> {

    // 开放的model，不需要检测权限
    // NOTE: 为方便自动测试集使用，此处设置为 public
    public static final String[] commonActions = {
            ConsoleConstants.MASTER_APP_NAME + "/" + ConsoleConstants.MODEL_NAME_index + "/index",
            ConsoleConstants.MASTER_APP_NAME + "/" + ConsoleConstants.MODEL_NAME_index + "/home"};

    private static final List<String> generalUris = new ArrayList<String>() {{
        for (String commonAction : commonActions) {
            add(RESTController.REST_PREFIX + "/" + ViewManager.htmlView + "/" + commonAction);
            add(RESTController.REST_PREFIX + "/" + ViewManager.jsonView + "/" + commonAction);
        }
    }};

    public static boolean isOpenedModelAction(String appModelAction) {
        appModelAction = wrapCheckingPath(appModelAction);
//        for (String oma : ServerXml.ConsoleRole.openedModelActions) {
//            if (appModelAction.startsWith(oma)) {
//                return true;
//            }
//        }
        return false;
    }

    public static boolean hasAppPermission(String loginUser, String appName) {//todo
        /*Set<XPermission> userRolePermissions = RoleCache.getUserRolePermissions(loginUser);
        if (userRolePermissions == null) {
            return false;
        }
        boolean hasPermission = false;
        for (XPermission permission : userRolePermissions) {
            hasPermission = permission.checkAppPermission(appName);
            if (hasPermission) {
                break;
            }
        }*/

        return true;
    }

    private static final String[] basicActionNames = { // 有任何其它权限的，则 附带打开其读 权限
            EditModel.ACTION_NAME_EDIT,
            ShowModel.ACTION_NAME_SHOW,
            ListModel.ACTION_NAME_LIST,
            AddModel.ACTION_NAME_CREATE,
            DownloadModel.ACTION_NAME_DOWNLOADLIST,
            ConsoleUtil.ACTION_NAME_TARGET
    };

    // note: this is a simply impl ...
    private static boolean hasModelActionPermission0(String appModelAction, String user) {
        for (String commonAction : commonActions) {
            if (commonAction.equals(appModelAction)) {
                return true;
            }
        }

        if (isOpenedModelAction(appModelAction)) {
            return true;
        }

//        if (ServerXml.ConsoleRole.isRootUser(user)) {
//            return true;
//        }

        String[] ma = appModelAction.split("/");
//        if (ma.length == 3) {
//            Set<XPermission> userRolePermissions = RoleCache.getUserRolePermissions(user);
//            if (userRolePermissions == null) {
//                return false;
//            }
//            String checkApp = ma[0];
//            String checkModel = ma[1];
//            String checkAction = ma[2];
//            boolean hasPermission;
//            for (XPermission permission : userRolePermissions) {
//                if (permission == null) {
//                    continue;
//                }
//                hasPermission = permission.checkPermission(checkApp, checkModel, checkAction, false);
//
//                if (!hasPermission) {
//                    // 自动追加权限
//                    Map<String, String> actionNameMap = new HashMap<String, String>() {{
//                        put(AddModel.ACTION_NAME_CREATE, AddModel.ACTION_NAME_ADD);
//                        put(DownloadModel.ACTION_NAME_DOWNLOADLIST, DownloadModel.ACTION_NAME_DOWNLOADFILE);
//                    }};
//                    for (Map.Entry<String, String> entry : actionNameMap.entrySet()) {
//                        if (checkAction.equals(entry.getKey())) {
//                            hasPermission = permission.checkPermission(checkApp, checkModel, entry.getValue(), false);
//                            break;
//                        }
//                    }
//                }
//
//                if (hasPermission) {
//                    return true;
//                }
//            }
//        }

        return false;
    }

    public static Model[] getLoginUserAppMenuModels(String loginUser, String appName) {
//        Set<XPermission> userRolePermissions = RoleCache.getUserRolePermissions(loginUser);
//        if (userRolePermissions == null) {
//            return new Model[0];
//        }

        ModelManager modelManager = ConsoleUtil.getModelManager(appName);
        if (modelManager == null) {
            return new Model[0];
        }

//        Set<Model> menuModels = new HashSet<>();
//        for (XPermission xPermission : userRolePermissions) {
//            if (xPermission == null) {
//                continue;
//            }
//
//            Set<String> roleModels = xPermission.getRoleModels(appName);
//            if (roleModels != null) {
//                for (String model : roleModels) {
//                    menuModels.add(modelManager.getModel(model));
//                }
//            }
//        }
//        return menuModels.toArray(new Model[0]);

        List<Model> models = new ArrayList<>();
        for (String modelName : modelManager.getModelNames()) {
            models.add(modelManager.getModel(modelName));
        }
        return models.toArray(new Model[0]);
    }

    public static boolean canAccess(String targetType, String targetName, String modelAction, String user) {
        return true;
    }

    public static boolean canAccess(String targetType, String appModelAction, String user) {
        return true;
    }

    public static boolean canAccess(String targetType, String targetName, String modelAction, String user, boolean skipClusterChecking) {
        return true;
    }

    public static boolean canAccess(String targetType, String appModelAction, String user, boolean skipClusterChecking) {
        return true;
    }

    public static boolean isNoNeedPermissionUri(HttpServletRequest request) {
//        String servletPathAndPathInfo = ConsoleUtil.retrieveServletPathAndPathInfo(request);
//
//        // 需要能调用加密接口，才能进行修改密码，故此设定：免权限的接口不需要修改密码即可认证。
//        String check = wrapCheckingPath(servletPathAndPathInfo);
//        for (String uri : generalUris) {
//            if (check.startsWith(uri)) {
//                return true;
//            }
//        }

        return false;
    }

    private static String wrapCheckingPath(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        return uri;
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.servletRequest;
        HttpServletResponse httpServletResponse = context.servletResponse;

        if (AccessControl.isNoNeedPermissionUri(httpServletRequest)) {
            return true;
        }

        String user = LoginManager.getLoginUser(httpServletRequest.getSession(false));
        if (StringUtil.notBlank(user)) {
            List<String> rest = RESTController.retrieveRestPathInfo(httpServletRequest);
            if (rest.size() >= 5) {
                String targetType = rest.get(1);
                String targetName = rest.get(2);
                String model = rest.get(3);
                String action = rest.get(4);
                String detectRest = model + "/" + action;
                if (canAccess(targetType, targetName, detectRest, user)) {
                    return true;
                }
            }
        }

//    todo    String msg = I18n.getString(qingzhou.framework.api.Constants.MASTER_APP_NAME, "page.error.permission.deny");
        String msg = "page.error.permission.deny";
        httpServletResponse.getWriter().print(JsonView.buildErrorResponse(msg));
        //        response.setStatus(HttpServletResponse.SC_FORBIDDEN);// 会引起命令行403，拿不到json提示信息
        //        response.sendError(HttpServletResponse.SC_FORBIDDEN);// 会引起上面的消息不传输到客户端或客户端http客户端有异常
        return false;
    }

}
