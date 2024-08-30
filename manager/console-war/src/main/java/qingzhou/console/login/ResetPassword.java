package qingzhou.console.login;

import qingzhou.api.Lang;
import qingzhou.config.Security;
import qingzhou.config.User;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SecurityFilter;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.SystemControllerContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.List;

public class ResetPassword implements Filter<SystemControllerContext> {
    static {
        I18n.addKeyI18n("page.warn.setpassword", new String[]{"请先重置默认密码", "en:Please reset your default password first"});
        I18n.addKeyI18n("password.max", new String[]{"已达到密码最长使用期限 %s 天，上次修改时间为：%s", "en:The maximum password age of %s days has been reached, last modified: %s"});
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.req;
        HttpServletResponse httpServletResponse = context.resp;

        if (SecurityFilter.skipPermissions(httpServletRequest)) return true;

        List<String> rest = RESTController.retrieveRestPathInfo(httpServletRequest);
        if (rest.size() < 5) return true;

        String model = rest.get(3);
        String action = rest.get(4);

        String user = LoginManager.getLoginUser(httpServletRequest.getSession(false));

        String msgI18nKey = user == null ? null : needReset(user);
        if (user != null && msgI18nKey != null) { // 例如加密工具不需要登录时候 user == null
            if (DeployerConstants.PASSWORD_MODEL.equals(model)) {
                if (DeployerConstants.EDIT_ACTION.equals(action)
                        || DeployerConstants.UPDATE_ACTION.equals(action)) { // 允许访问重置密码的 uri
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
                    "/" + DeployerConstants.APP_MANAGE +
                    "/" + DeployerConstants.MASTER_APP +
                    "/" + DeployerConstants.PASSWORD_MODEL +
                    "/" + DeployerConstants.EDIT_ACTION +
                    "/" + user +
                    "?" + RESTController.MSG_FLAG + "=" + msgI18nKey));
            return false;
        }

        return true;
    }

    private String needReset(String user) throws Exception {
        User u = SystemController.getConsole().getUser(user);
        if (u == null) return null;

        if (u.isChangePwd()) return "page.warn.setpassword";

        Security security = SystemController.getConsole().getSecurity();
        int maxAge = security.getPasswordMaxAge();
        if (maxAge > 0) {
            String passwordLastModified = u.getPasswordLastModified();
            if (passwordLastModified != null && !passwordLastModified.isEmpty()) {
                long time = new SimpleDateFormat(DeployerConstants.PASSWORD_LAST_MODIFIED_DATE_FORMAT).parse(passwordLastModified).getTime();
                long max = time + maxAge * ConsoleConstants.DAY_MILLIS_VALUE;
                if (System.currentTimeMillis() > max) {
                    return "password.max," + maxAge + "," + passwordLastModified;
                }
            }
        }

        return null;
    }
}
