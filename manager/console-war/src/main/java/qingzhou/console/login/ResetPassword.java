package qingzhou.console.login;

import qingzhou.api.Lang;
import qingzhou.config.Security;
import qingzhou.config.User;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.AccessControl;
import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.List;

public class ResetPassword implements Filter<HttpServletContext> {
    private static final String setPasswordMsg = "page.warn.setpassword";

    static {
        ConsoleI18n.addI18n(setPasswordMsg, new String[]{"请先重置默认密码", "en:Please reset your default password first"});
        ConsoleI18n.addI18n("password.max", new String[]{"已达到密码最长使用期限 %s 天，上次修改时间为：%s", "en:The maximum password age of %s days has been reached, last modified: %s"});
    }

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.req;
        HttpServletResponse httpServletResponse = context.resp;

        if (AccessControl.isNoNeedPermissionUri(httpServletRequest)) {
            return true;
        }

        List<String> rest = RESTController.retrieveRestPathInfo(httpServletRequest);
        if (rest.size() < 5) {
            return true;
        }

        String model = rest.get(3);
        String action = rest.get(4);

        String user = LoginManager.getLoginUser(httpServletRequest.getSession(false));

        String msgI18nKey = user == null ? null : needReset(user);
        if (user != null && msgI18nKey != null) { // 例如加密工具不需要登录时候 user == null
            if (ConsoleConstants.MODEL_NAME_password.equals(model)) {
                if ("edit".equals(action)
                        || "update".equals(action)) { // 允许访问重置密码的 uri
                    return true;
                }
            }

            String viewName = "/" + rest.get(0);
            String toJson = JsonView.responseErrorJson(httpServletResponse, LoginManager.retrieveI18nMsg(msgI18nKey));
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                httpServletResponse.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                httpServletResponse.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, PageBackendService.encodeId(toJson));
            }

            httpServletResponse.sendRedirect(PageBackendService.encodeURL(httpServletResponse, httpServletRequest.getContextPath() +
                    RESTController.REST_PREFIX +
                    viewName +
                    "/" + DeployerConstants.MANAGE_TYPE_APP +
                    "/" + DeployerConstants.MASTER_APP_NAME +
                    "/" + ConsoleConstants.MODEL_NAME_password +
                    "/edit" +
                    "/" + user +
                    "?" + RESTController.MSG_FLAG + "=" + msgI18nKey));
            return false;
        }

        return true;
    }

    private String needReset(String user) throws Exception {
        User u = SystemController.getConsole().getUser(user);
        if (u == null) {
            return null;
        }

        if (u.isChangeInitPwd()) {
            return setPasswordMsg;
        }

        Security security = SystemController.getConsole().getSecurity();
        if (security.isEnablePasswordAge()) {
            String passwordLastModifiedTime = u.getPasswordLastModifiedTime();
            if (passwordLastModifiedTime != null && !passwordLastModifiedTime.isEmpty()) {
                long time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(passwordLastModifiedTime).getTime();
                int maxAge = security.getPasswordMaxAge();
                if (maxAge > 0) {
                    long max = time + maxAge * ConsoleConstants.DAY_MILLIS_VALUE;
                    if (System.currentTimeMillis() > max) {
                        return "password.max," + maxAge + "," + passwordLastModifiedTime;
                    }
                }
            }
        }

        return null;
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

}
