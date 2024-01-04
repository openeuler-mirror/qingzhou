package qingzhou.console.login;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.ServerXml;
import qingzhou.console.controller.rest.AccessControl;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.controller.system.HttpServletContext;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.Lang;
import qingzhou.framework.pattern.Filter;
import qingzhou.console.ConsoleConstants;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.ServerUtil;
import qingzhou.framework.util.TimeUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class ResetPassword implements Filter<HttpServletContext> {
    private static final String setPasswordMsg = "page.warn.setpassword";
    private static final String set2FAMsg = "page.warn.set2fa";

    static {
        ConsoleContext master = ServerUtil.getMasterConsoleContext();
        if (master != null) {
            master.addI18N(setPasswordMsg, new String[]{"请先重置默认密码", "en:Please reset your default password first"});
            master.addI18N(set2FAMsg, new String[]{"请先扫描二维码绑定双因子认证密钥", "en:Please scan the QR code to bind the two-factor authentication key"});
            master.addI18N("password.max", new String[]{"已达到密码最长使用期限 %s 天，上次修改时间为：%s", "en:The maximum password age of %s days has been reached, last modified: %s"});
        }
    }

    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest httpServletRequest = context.req;
        HttpServletResponse httpServletResponse = context.resp;

        if (AccessControl.isNoNeedPermissionUri(httpServletRequest)) {
            return true;
        }

        List<String> rest = RESTController.retrieveRestPathInfo(httpServletRequest);
        if (rest.size() < 6) {
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
            String toJson = JsonView.buildErrorResponse(LoginManager.retrieveI18nMsg(msgI18nKey));
            httpServletResponse.getWriter().print(toJson);// 重定向，会丢失body里的消息
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                httpServletResponse.setHeader(qingzhou.framework.api.Constants.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                httpServletResponse.setHeader(qingzhou.framework.api.Constants.RESPONSE_HEADER_MSG_KEY, ConsoleSDK.encodeId(toJson));
            }

            httpServletResponse.sendRedirect(ConsoleUtil.encodeRedirectURL(httpServletRequest, httpServletResponse, httpServletRequest.getContextPath() +
                    RESTController.REST_PREFIX +
                    viewName +
                    "/" + ConsoleConstants.MODEL_NAME_node +
                    "/" + qingzhou.framework.api.Constants.MASTER_APP_NAME +
                    "/" + ConsoleConstants.MODEL_NAME_password +
                    "/edit" +
                    "/" + user +
                    "?" + RESTController.MSG_FLAG + "=" + msgI18nKey));
            return false;
        }

        return true;
    }

    private String needReset(String user) {
        Map<String, String> userP = ServerXml.get().user(user);
        if (userP == null) {
            return null;
        }

        if (Boolean.parseBoolean(userP.get("enable2FA"))) {
            if (!Boolean.parseBoolean(userP.get("bound2FA"))) {
                return set2FAMsg;
            }
        }

        if (Boolean.parseBoolean(userP.get("changeInitPwd"))) {
            return setPasswordMsg;
        }

        if (Boolean.parseBoolean(userP.get("enablePasswordAge"))) {
            String passwordLastModifiedTime = userP.get("passwordLastModifiedTime");
            if (passwordLastModifiedTime != null) {
                long time;
                try {
                    time = new SimpleDateFormat(ConsoleConstants.DATE_FORMAT).parse(passwordLastModifiedTime).getTime();
                } catch (ParseException e) {
                    throw ExceptionUtil.unexpectedException(e);
                }
                String maxAge = userP.get("passwordMaxAge");
                if (maxAge != null && !maxAge.equals("0")) {
                    long max = time + Integer.parseInt(maxAge) * ConsoleConstants.DAY_MILLIS_VALUE;
                    if (TimeUtil.getCurrentTime() > max) {
                        return "password.max," + maxAge + "," + passwordLastModifiedTime;
                    }
                }
            }
        }

        return null;
    }
}
