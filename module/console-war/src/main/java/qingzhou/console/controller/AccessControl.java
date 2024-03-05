package qingzhou.console.controller;

import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.console.ConsoleI18n;
import qingzhou.console.I18n;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.framework.Constants;
import qingzhou.framework.app.App;
import qingzhou.framework.config.Config;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

public class AccessControl implements Filter<HttpServletContext> {
    static {
        ConsoleI18n.addI18n("page.error.permission.deny", new String[]{"对不起，您无权访问该资源", "en:Sorry, you do not have access to this resource"});
    }

    private static final List<String> masterAppModels = Arrays.asList("user", "version", "node");

    public static ModelData[] getLoginUserAppMenuModels(String loginUser, String appName) {
        ModelManager modelManager = SystemController.getAppMetadata(appName).getModelManager();
        if (modelManager == null) {
            return new ModelData[0];
        }

        List<ModelData> models = new ArrayList<>();
        for (String modelName : modelManager.getModelNames()) {
            models.add(modelManager.getModel(modelName));
        }

        if (!Constants.DEFAULT_ADMINISTRATOR.equals(loginUser) && App.SYS_APP_MASTER.equals(appName)) {
            models = models.stream().filter(model -> !masterAppModels.contains(model.name())).collect(Collectors.toList());
        }

        return models.toArray(new ModelData[0]);
    }

    public static boolean canAccess(String appName, String modelAction, String user) {
        String[] ma = modelAction.split("/");
        String checkModel = ma[0];
        String checkAction = ma[1];
        if (ma.length == 2) {
            ModelManager modelManager = SystemController.getAppMetadata(appName).getModelManager();
            ModelActionData modelAction1 = modelManager.getModelAction(checkModel, checkAction);
            return modelAction1 != null;
        }

        return nodePermission(appName, user);
    }

    public static boolean nodePermission(String appName, String user) {
        if (Constants.DEFAULT_ADMINISTRATOR.equals(user)) {
            return true;
        }

        Config config = SystemController.getConfig();
        Map<String, String> userPro = config.getConfig("/user[@id='" + user + "']");
        String userNodes = userPro.getOrDefault("nodes", "");
        Set<String> userNodeSet = Arrays.stream(userNodes.split(","))
                .filter(node -> node != null && !node.trim().isEmpty())
                .map(String::trim).collect(Collectors.toSet());

        Map<String, String> app = config.getConfig("/app[@id='" + appName + "']");
        String appNodes = app.getOrDefault("nodes", "");

        return Arrays.stream(appNodes.split(",")).map(String::trim).anyMatch(userNodeSet::contains);
    }

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
//        String check = wrapCheckingPath(servletPathAndPathInfo);
//        for (String uri : generalUris) {
//            if (check.startsWith(uri)) {
//                return true;
//            }
//        }

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
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.req;
        HttpServletResponse httpServletResponse = context.resp;

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

        String msg = ConsoleI18n.getI18n(I18n.getI18nLang(), "page.error.permission.deny");
        JsonView.responseErrorJson(httpServletResponse, msg);
        return false;
    }
}
