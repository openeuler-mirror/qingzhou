package qingzhou.console.controller;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class AccessControl implements Filter<HttpServletContext> {
    static {
        ConsoleI18n.addI18n("page.error.permission.deny", new String[]{"对不起，您无权访问该资源", "en:Sorry, you do not have access to this resource"});
    }

    private static final List<String> generalUris = new ArrayList<String>() {{
        add(RESTController.INDEX_PATH);
    }};

    public static boolean canAccess(String appName, String modelAction, String user) throws Exception {
        String[] ma = modelAction.split("/");
        String checkModel = ma[0];
        String checkAction = ma[1];
        if (ma.length == 2) {
            ModelInfo modelInfo = SystemController.getAppInfo(appName).getModelInfo(checkModel);
            if (modelInfo == null || modelInfo.isHidden()) {
                return false;
            }
            ModelActionInfo actionInfo = modelInfo.getModelActionInfo(checkAction);
            if (actionInfo == null || actionInfo.isDisable()) {
                return false;
            }
            if (DeployerConstants.MASTER_APP_PASSWORD_MODEL_NAME.equals(checkModel)) {
                return true;
            }
        }

        return true;//nodePermission(appName, user);
    }

//    public static boolean nodePermission(String appName, String user) throws Exception {
//        if ("qingzhou".equals(user)) {
//            return true;
//        }
//
//        Config config = SystemController.getConfig();
//        Map<String, String> userPro = config.getDataById("user", user);
//        String userNodes = userPro.getOrDefault("nodes", "");
//        Set<String> userNodeSet = Arrays.stream(userNodes.split(","))
//                .filter(node -> node != null && !node.trim().isEmpty())
//                .map(String::trim).collect(Collectors.toSet());
//
//        Map<String, String> app = config.getDataById("app", appName);
//        String appNodes = app.getOrDefault("nodes", "");
//
//        return Arrays.stream(appNodes.split(",")).map(String::trim).anyMatch(userNodeSet::contains);
//    }

    public static boolean isNoNeedPermissionUri(HttpServletRequest request) {
        String servletPathAndPathInfo = RESTController.retrieveServletPathAndPathInfo(request);

        for (String uri : LoginManager.noLoginCheckUris) {
            if (servletPathAndPathInfo.equals(uri)) {
                return true;
            }
        }

        if (isNoLoginCheckUris(servletPathAndPathInfo)) {
            return true;
        }
        // 需要能调用加密接口，才能进行修改密码，故此设定：免权限的接口不需要修改密码即可认证。
        String check = wrapCheckingPath(servletPathAndPathInfo);
        for (String uri : generalUris) {
            if (check.startsWith(uri)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isNoLoginCheckUris(String path) {
        if (path.startsWith("/static/")) {
            for (String suffix : LoginManager.STATIC_RES_SUFFIX) {
                if (path.endsWith(suffix)) {
                    return true;
                }
            }
        }

        // 远程实例注册
        return path.startsWith("/rest/json/app/" + DeployerConstants.MASTER_APP_NAME + "/" + DeployerConstants.MASTER_APP_INSTANCE_MODEL_NAME + "/checkRegistry") ||
                path.startsWith("/rest/json/app/" + DeployerConstants.MASTER_APP_NAME + "/" + DeployerConstants.MASTER_APP_INSTANCE_MODEL_NAME + "/register");
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
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.req;
        HttpServletResponse httpServletResponse = context.resp;

        if (AccessControl.isNoNeedPermissionUri(httpServletRequest)) {
            return true;
        }

        String user = LoginManager.getLoginUser(httpServletRequest.getSession(false));
        if (user != null) {
            List<String> rest = RESTController.retrieveRestPathInfo(httpServletRequest);
            if (rest.size() >= 5) {
                String appName = PageBackendService.getAppName(rest.get(1), rest.get(2));
                String model = rest.get(3);
                String action = rest.get(4);
                String detectRest = model + "/" + action;
                if (canAccess(appName, detectRest, user)) {
                    return true;
                }
            }
        }

        String msg = ConsoleI18n.getI18n(I18n.getI18nLang(), "page.error.permission.deny");
        JsonView.responseErrorJson(httpServletResponse, msg);
        return false;
    }
}
