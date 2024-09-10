package qingzhou.console.controller.rest;

import qingzhou.api.Constants;
import qingzhou.api.Lang;
import qingzhou.config.Console;
import qingzhou.config.User;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;

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

        // 登陆时候检查一次，已重置过密码，本次会话不必再检查
        String RESET_OK_FLAG = "RESET_OK_FLAG";
        if (session.getAttribute(RESET_OK_FLAG) != null) return true;

        String user = LoginManager.getLoginUser(servletRequest);
        String resetPwdInfo = checkResetPwdInfo(user);
        if (resetPwdInfo == null) {
            session.setAttribute(RESET_OK_FLAG, RESET_OK_FLAG);// 成功登录
            return true;
        }

        // 不重置密码，允许进入主页
        if (DeployerConstants.MODEL_INDEX.equals(context.request.getModel())) {
            if (DeployerConstants.ACTION_INDEX.equals(context.request.getAction())) {
                return true;
            }
        }

        // 不重置密码，允许进入修改密码页
        if (DeployerConstants.MODEL_PASSWORD.equals(context.request.getModel())) {
            if (Constants.ACTION_EDIT.equals(context.request.getAction())
                    || Constants.ACTION_UPDATE.equals(context.request.getAction())) { // 允许访问重置密码的 uri
                return true;
            }
        }

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
                "/" + Constants.ACTION_EDIT +
                "/" + user +
                "?" + RESTController.MSG_FLAG + "=" + resetPwdInfo));

        return false;
    }

    private String checkResetPwdInfo(String user) throws Exception {
        Console console = SystemController.getConsole();

        User u = console.getUser(user);
        if (u.isChangePwd()) return "page.warn.setpassword";

        int maxAge = console.getSecurity().getPasswordMaxAge();
        if (maxAge > 0) {
            String passwordLastModified = u.getPasswordLastModified();
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
