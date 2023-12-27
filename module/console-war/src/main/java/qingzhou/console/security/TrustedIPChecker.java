package qingzhou.console.security;

import qingzhou.console.controller.HttpServletContext;
import qingzhou.console.controller.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.app.I18n;
import qingzhou.framework.app.Lang;
import qingzhou.framework.pattern.Filter;
import qingzhou.console.util.Constants;
import qingzhou.console.util.IPUtil;
import qingzhou.console.ServerXml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TrustedIPChecker implements Filter<HttpServletContext> {
    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        if (trustedIP(request.getRemoteAddr())) {
            return true;
        }
        String msgKey = "client.trusted.not";
        String toJson = JsonView.buildErrorResponse(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, msgKey));
        if (I18n.getI18nLang() == Lang.en) { // header里只能英文
            response.setHeader(Constants.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
        } else {
            response.setHeader(Constants.RESPONSE_HEADER_MSG_KEY, ConsoleSDK.encodeId(toJson));
        }
        response.getWriter().print(toJson);// 如果后面紧接着重定向了，body 里面的消息会丢失，所以这里用了响应头传递信息
        response.sendRedirect(request.getContextPath() + LoginManager.LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + msgKey);
        return false;
    }

    public static boolean trustedIP(String clientIp) {
        if (IPUtil.isLocalIp(clientIp)) {
            return true;
        }
        return IPUtil.validateIps(ServerXml.get().trustedIP(), clientIp);
    }
}
