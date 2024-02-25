package qingzhou.console.controller;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelManager;
import qingzhou.config.Config;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

public class AccessControl implements Filter<HttpServletContext> {
    private static final List<String> masterAppModels = Arrays.asList("user", "version", "node");

    public static Model[] getLoginUserAppMenuModels(String loginUser, String appName) {
        ModelManager modelManager = ConsoleWarHelper.getAppStub(appName).getModelManager();
        if (modelManager == null) {
            return new Model[0];
        }

        List<Model> models = new ArrayList<>();
        for (String modelName : modelManager.getModelNames()) {
            models.add(modelManager.getModel(modelName));
        }

        if (!"qingzhou".equals(loginUser) && qingzhou.app.App.SYS_APP_MASTER.equals(appName)) {
            models = models.stream().filter(model -> !masterAppModels.contains(model.name())).collect(Collectors.toList());
        }

        return models.toArray(new Model[0]);
    }

    public static boolean canAccess(String appName, String modelAction, String user) {
        String[] ma = modelAction.split("/");
        String checkModel = ma[0];
        String checkAction = ma[1];
        if (ma.length == 2) {
            ModelManager modelManager = ConsoleWarHelper.getAppStub(appName).getModelManager();
            ModelAction modelAction1 = modelManager.getModelAction(checkModel, checkAction);
            return modelAction1 != null;
        }

        return nodePermission(appName, user);
    }

    public static boolean nodePermission(String appName, String user) {
        if ("qingzhou".equals(user)) {
            return true;
        }

        Config config = ConsoleWarHelper.getConfig();
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

//    todo    String msg = I18n.getString(qingzhou.api.Constants.SYS_APP_MASTER, "page.error.permission.deny");
        String msg = "page.error.permission.deny";
        httpServletResponse.getWriter().print(JsonView.buildErrorResponse(msg));
        //        response.setStatus(HttpServletResponse.SC_FORBIDDEN);// 会引起命令行403，拿不到json提示信息
        //        response.sendError(HttpServletResponse.SC_FORBIDDEN);// 会引起上面的消息不传输到客户端或客户端http客户端有异常
        return false;
    }
}
