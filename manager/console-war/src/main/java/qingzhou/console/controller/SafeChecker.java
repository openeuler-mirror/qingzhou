package qingzhou.console.controller;

import qingzhou.api.Lang;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.util.IPUtil;
import qingzhou.console.view.type.JsonView;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SafeChecker implements Filter<HttpServletContext> {
    @Override
    public boolean doFilter(HttpServletContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        String checkPath = RESTController.retrieveServletPathAndPathInfo(request);
        if (checkPath.equals(LoginManager.LOGIN_URI) && !trustedIP(request.getRemoteAddr())) {
            String msgKey = "client.trusted.not";
            String toJson = JsonView.responseErrorJson(response, ConsoleI18n.getI18n(I18n.getI18nLang(), msgKey));
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                response.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                response.setHeader(ConsoleConstants.RESPONSE_HEADER_MSG_KEY, PageBackendService.encodeId(toJson));
            }
            response.sendRedirect(request.getContextPath() + LoginManager.LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + msgKey);
            return false;
        }

        if (checkPath.endsWith(".jsp") || checkPath.endsWith(".jspx")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    public static boolean trustedIP(String clientIp) {
        if (IPUtil.isLocalIp(clientIp)) {
            return true;
        }
        try {
            String trustedIP = SystemController.getConsole().getSecurity().getTrustedIP();
            return validateIps(trustedIP, clientIp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean validateIps(String trustedPattern, String checkIp) throws UnknownHostException {
        if ("*".equals(trustedPattern)) {
            return true;
        }

        if (trustedPattern != null) {
            if (Pattern.matches(trustedPattern, checkIp)) {
                return true;
            }
        }

        boolean isOk = validateIpOrigin(trustedPattern, checkIp);
        if (!isOk) {
            System.err.println("Client IP ( " + checkIp + " ) was rejected because the trust mode is not satisfied: ( " + trustedPattern + " )");
            System.err.println("The client IP [ " + checkIp + " ] has been intercepted, and the currently configured trusted IP policy is: [" + trustedPattern + " ]");
        }
        return isOk;
    }

    private static boolean validateIpOrigin(String validIps, String ip) throws UnknownHostException {
        if (validIps == null) {
            return false;
        }

        String[] validates = validIps.split(",");
        for (String validIp : validates) {
            validIp = validIp.trim().toLowerCase();
            if (validIp.contains("*")) {
                if (matchIP(validIp, ip)) {
                    return true;
                }
            } else if (validIp.contains("-")) {
                long lo = ipToLong(InetAddress.getByName((validIp.split("-")[0]).trim()));
                long hi = ipToLong(InetAddress.getByName((validIp.split("-")[1]).trim()));
                long ipt = ipToLong(InetAddress.getByName(ip));
                if (ipt >= lo && ipt <= hi) {
                    return true;
                }
            } else {// add for exactIP ex:168.1.2.3
                if (validIp.equals(ip)) {
                    return true;
                } else {
                    if (validIp.contains(":")) {//ipv6
                        if (parseIpv6(validIp).equals(parseIpv6(ip))) {
                            return true;
                        }
                    }
                }

            }
        }

        return false;
    }

    private static long ipToLong(InetAddress ip) {
        long result = 0;

        byte[] ipAdds = ip.getAddress();

        for (byte b : ipAdds) {
            result <<= 8;
            result |= b & 0xff;
        }

        return result;
    }

    private static boolean matchIP(String ips, String ip) {
        if (ips.contains(".")) {
            String[] validips = ips.split("\\.");
            StringBuilder re = new StringBuilder();
            for (int j = 0; j < validips.length; j++) {
                String num = validips[j];
                if (!num.equals("*")) {
                    re.append(num).append(".");
                } else {
                    re.append("\\d{0,3}.");
                }
                if (j == validips.length - 1) {
                    re = new StringBuilder(re.substring(0, re.length() - 1));
                }
            }

            Pattern pattern = Pattern.compile(re.toString());
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } else if (ips.contains(":")) {
            Pattern pattern = Pattern.compile(parseIpv6(ips));
            Matcher matcher = pattern.matcher(parseIpv6(ip));
            return matcher.matches();
        } else {
            return false;
        }
    }


    private static String parseIpv6(String ipv6Address) {
        String[] ips = ipv6Address.split(":");
        StringBuilder realIp = new StringBuilder();
        for (int j = 0; j < ips.length; j++) {
            String num = ips[j];
            if (!num.equals("*")) {
                if (num.isEmpty()) {
                    int len = 9 - ips.length;//"" is one so:8+1
                    for (int t = 0; t < len; t++) {
                        realIp.append("0:");
                    }
                } else {
                    realIp.append(num).append(":");
                }
            } else {
                realIp.append("[\\da-z]{0,4}:");
            }
            if (j == ips.length - 1) {
                realIp = new StringBuilder(realIp.substring(0, realIp.length() - 1));
            }
        }
        return realIp.toString();
    }
}
