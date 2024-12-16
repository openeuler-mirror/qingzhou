package qingzhou.console.controller.rest;

import java.text.SimpleDateFormat;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import qingzhou.api.Lang;
import qingzhou.api.type.Update;
import qingzhou.config.console.User;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.core.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;

public class ResetPassword implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("page.warn.setpassword", new String[]{"请先重置默认密码", "en:Please reset your default password first"});
        I18n.addKeyI18n("password.max", new String[]{"已达到密码最长使用期限 %s 天，上次修改时间为：%s", "en:The maximum password age of %s days has been reached, last modified: %s"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        HttpServletRequest servletRequest = context.req;
        HttpServletResponse servletResponse = context.resp;

        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            if (LoginManager.isOpenUris(RESTController.getReqUri(servletRequest))) {
                return true;
            }

            LoginManager.forwardToLoginJsp(servletRequest, servletResponse);
            return false;
        }

        // 性能优化：登陆时候检查一次，已重置过密码，本次会话不必再检查
        String RESET_OK_FLAG = "RESET_OK_FLAG";
        if (session.getAttribute(RESET_OK_FLAG) != null) return true;

        String model = context.request.getModel();
        if (DeployerConstants.APP_SYSTEM.equals(context.request.getApp())) {
            if (DeployerConstants.OPEN_SYSTEM_MODELS.contains(model)) {
                return true;
            }
            Set<String> actions = DeployerConstants.OPEN_SYSTEM_MODEL_ACTIONS.get(model);
            if (actions != null && actions.contains(context.request.getAction())) {
                return true;
            }
        } else {
            if (DeployerConstants.OPEN_NONE_SYSTEM_MODELS.contains(model)) {
                return true;
            }
        }

        User user = LoginManager.getLoggedUser(session);
        String resetPwdInfo = checkResetPwdInfo(user);
        if (resetPwdInfo == null) { // 无需重置密码
            session.setAttribute(RESET_OK_FLAG, true);
            return true;
        } else { // 提示需要重置密码
            String toJson = JsonView.responseErrorJson(servletResponse, LoginManager.retrieveI18nMsg(resetPwdInfo));
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                servletResponse.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                servletResponse.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, RESTController.encodeId(toJson));
            }
            servletResponse.sendRedirect(RESTController.encodeURL(servletResponse, servletRequest.getContextPath() +
                    DeployerConstants.REST_PREFIX +
                    "/" + context.request.getView() +
                    "/" + DeployerConstants.APP_SYSTEM +
                    "/" + DeployerConstants.MODEL_PASSWORD +
                    "/" + Update.ACTION_EDIT +
                    "/" + user.getName() +
                    "?" + RESTController.MSG_FLAG + "=" + resetPwdInfo));
            return false;
        }
    }

    private String checkResetPwdInfo(User user) throws Exception {
        if (user.isChangePwd()) return "page.warn.setpassword";

        int maxAge = SystemController.getConsole().getSecurity().getPasswordMaxAge();
        if (maxAge > 0) {
            String passwordLastModified = user.getPasswordLastModified();
            if (passwordLastModified != null && !passwordLastModified.isEmpty()) {
                long time = new SimpleDateFormat(DeployerConstants.PASSWORD_LAST_MODIFIED_DATE_FORMAT).parse(passwordLastModified).getTime();
                long DAY_MILLIS_VALUE = 24 * 60 * 60 * 1000; // 一天的毫秒值
                long max = time + maxAge * DAY_MILLIS_VALUE;
                if (System.currentTimeMillis() > max) {
                    return "password.max," + maxAge + "," + passwordLastModified;
                }
            }
        }

        return null;
    }
}
