package qingzhou.console.controller.rest;

import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AccessControl implements Filter<RestContext> {
    private static final List<String> masterAppModels = Arrays.asList("user", "version", "node");

    public static Model[] getLoginUserAppMenuModels(String loginUser, String appName) {
        ModelManager modelManager = PageBackendService.getModelManager(appName);
        if (modelManager == null) {
            return new Model[0];
        }

        List<Model> models = new ArrayList<>();
        for (String modelName : modelManager.getModelNames()) {
            models.add(modelManager.getModel(modelName));
        }

        if (!"qingzhou".equals(loginUser) && FrameworkContext.SYS_APP_MASTER.equals(appName)) {
            models = models.stream().filter(model -> !masterAppModels.contains(model.name())).collect(Collectors.toList());
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
