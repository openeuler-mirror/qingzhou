package qingzhou.console.controller;

import qingzhou.api.Lang;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.logger.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrustIpCheck implements Filter<SystemControllerContext> {
    static {
        I18n.addKeyI18n("client.trusted.not", new String[]{"该操作仅限于在服务器本机或受信任的IP上执行，受信任IP的设置方式请参考相关手册", "en:This operation can only be performed on the local server or on a trusted IP. Please refer to the manual for the setting method of the trusted IP"});
    }

    @Override
    public boolean doFilter(SystemControllerContext context) throws Exception {
        HttpServletRequest request = context.req;
        HttpServletResponse response = context.resp;

        String checkPath = RESTController.getReqUri(request);
        if (checkPath.equals(LoginManager.LOGIN_URI) && notTrustedIp(request.getRemoteAddr())) {
            String msgKey = "client.trusted.not";
            String toJson = JsonView.responseErrorJson(response, I18n.getKeyI18n(msgKey));
            if (I18n.getI18nLang() == Lang.en) { // header里只能英文
                response.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, toJson);// 重定向，会丢失body里的消息，所以用header
            } else {
                response.setHeader(LoginManager.RESPONSE_HEADER_MSG_KEY, RESTController.encodeId(toJson));
            }
            response.sendRedirect(request.getContextPath() + LoginManager.LOGIN_PATH + "?" + RESTController.MSG_FLAG + "=" + msgKey);
            return false;
        }

        return true;
    }

    public static boolean notTrustedIp(String clientIp) {
        if (Utils.isLocalIp(clientIp)) {
            return false;
        }
        try {
            String trustedIp = SystemController.getConsole().getSecurity().getTrustedIp();
            return !validateIps(trustedIp, clientIp);
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
            SystemController.getService(Logger.class).info("Client IP ( " + checkIp + " ) was rejected because the trust mode is not satisfied: ( " + trustedPattern + " )");
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
            } else { // add for exactIP ex:168.1.2.3
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
                if (!"*".equals(num)) {
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
            if (!"*".equals(num)) {
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
