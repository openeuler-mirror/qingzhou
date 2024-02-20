package qingzhou.console.controller.rest;

import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class AccessControl implements Filter<RestContext> {

    public static Model[] getLoginUserAppMenuModels(String loginUser, String appName) {
//        Set<XPermission> userRolePermissions = RoleCache.getUserRolePermissions(loginUser);
//        if (userRolePermissions == null) {
//            return new Model[0];
//        }

        ModelManager modelManager = PageBackendService.getModelManager(appName);
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

    public static boolean canAccess(String appName, String modelAction, String user) {
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
                String appName = rest.get(2);
                String model = rest.get(3);
                String action = rest.get(4);
                String detectRest = model + "/" + action;
                if (canAccess(appName, detectRest, user)) {
                    return true;
                }
            }
        }

//    todo    String msg = I18n.getString(qingzhou.framework.api.Constants.SYS_APP_MASTER, "page.error.permission.deny");
        String msg = "page.error.permission.deny";
        httpServletResponse.getWriter().print(JsonView.buildErrorResponse(msg));
        //        response.setStatus(HttpServletResponse.SC_FORBIDDEN);// 会引起命令行403，拿不到json提示信息
        //        response.sendError(HttpServletResponse.SC_FORBIDDEN);// 会引起上面的消息不传输到客户端或客户端http客户端有异常
        return false;
    }
}
