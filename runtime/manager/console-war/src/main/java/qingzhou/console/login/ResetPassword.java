package qingzhou.console.login;

import qingzhou.api.Lang;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.ServerXml;
import qingzhou.console.controller.AccessControl;
import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.type.JsonView;
import qingzhou.deployer.App;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class ResetPassword implements Filter<HttpServletContext> {
    private static final String setPasswordMsg = "page.warn.setpassword";
    private static final String set2FAMsg = "page.warn.set2fa";

    static {
        ConsoleI18n.addI18n(setPasswordMsg, new String[]{"请先重置默认密码", "en:Please reset your default password first"});
        ConsoleI18n.addI18n(set2FAMsg, new String[]{"请先扫描二维码绑定双因子认证密钥", "en:Please scan the QR code to bind the two-factor authentication key"});
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
            String toJson = JsonView.responseErrorJson(httpServletResponse, LoginManager.retrieveI18nMsg(msgI18nKey));
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                httpServletResponse.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                httpServletResponse.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, PageBackendService.encodeId(toJson));
            }

            httpServletResponse.sendRedirect(PageBackendService.encodeURL(httpServletResponse, httpServletRequest.getContextPath() +
                    RESTController.REST_PREFIX +
                    viewName +
                    "/" + ConsoleConstants.MODEL_NAME_node +
                    "/" + App.SYS_APP_MASTER +
                    "/" + ConsoleConstants.MODEL_NAME_password +
                    "/edit" +
                    "/" + user +
                    "?" + RESTController.MSG_FLAG + "=" + msgI18nKey));
            return false;
        }

        return true;
    }

    private String needReset(String user) throws Exception {
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
                long time = new SimpleDateFormat(",").parse(passwordLastModifiedTime).getTime();
                String maxAge = userP.get("passwordMaxAge");
                if (maxAge != null && !maxAge.equals("0")) {
                    long max = time + Integer.parseInt(maxAge) * ConsoleConstants.DAY_MILLIS_VALUE;
                    if (System.currentTimeMillis() > max) {
                        return "password.max," + maxAge + "," + passwordLastModifiedTime;
                    }
                }
            }
        }

        return null;
    }
}
