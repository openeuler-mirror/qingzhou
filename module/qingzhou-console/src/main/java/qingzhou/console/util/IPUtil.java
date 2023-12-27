package qingzhou.console.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPUtil {
    private static List<String> localIpsAsFull;
    private static List<String> inetAddressList;
    private static String defaultIp;
    private static Set<String> localIps;
    private static Set<String> localIps0;

    public static boolean validateIps(String trustedPattern, String checkIp) {
        if ("*".equals(trustedPattern)) {
            return true;
        }

        if (StringUtil.notBlank(trustedPattern)) {
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

    private static boolean validateIpOrigin(String validIps, String ip) {
        if (StringUtil.isBlank(validIps)) {
            return false;
        }
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        System.err.println("Client IP (" + ip + ") was rejected because the trust mode is not satisfied: " + validIps);
        return false;
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

    private static long ipToLong(InetAddress ip) {
        long result = 0;

        byte[] ipAdds = ip.getAddress();

        for (byte b : ipAdds) {
            result <<= 8;
            result |= b & 0xff;
        }

        return result;
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

    public static boolean isIpOrHost(String newValue) {
        if (StringUtil.isBlank(newValue)) {
            return false;
        }

        // 兼容这个情况： newValue == 0.0.0.0aa
        try {
            InetAddress.getByName(newValue).getHostAddress();//ITAIT-3980 ::1简写变成完整名称
            return true;
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    public static List<String> getLocalIpsAsFull() {//Set<String> ips = Utils.getLocalIps();//can not get add ips.use the local method getLocalIp
        if (localIpsAsFull == null) {
            localIpsAsFull = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                while (en.hasMoreElements()) {
                    NetworkInterface intf = en.nextElement();
                    Enumeration<InetAddress> enAddr = intf.getInetAddresses();
                    while (enAddr.hasMoreElements()) {
                        localIpsAsFull.add(enAddr.nextElement().getHostAddress().split("%")[0]);
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return localIpsAsFull;
    }

    public static boolean isPortOpened(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(host), port), 1000);// 如果超时时间太长，会导致创建域的页面卡顿！！
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLocalIp(String ip) {
        if (localIps0 == null) {
            try {
                localIps0 = new HashSet<>();
                localIps0.addAll(getLocalIps());
                localIps0.add("127.0.0.1");
                localIps0.add("localhost");
                localIps0.add("::1");
                localIps0.add(InetAddress.getByName("::1").getHostAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return localIps0.contains(ip);
    }

    public static String getDefaultIp() {
        if (defaultIp == null) {
            for (String ip : getLocalInetAddress()) {
                if (ip != null) {
                    defaultIp = ip;
                    break;
                }
            }
        }
        return defaultIp;
    }

    public static Set<String> getLocalIps() {
        if (localIps == null) {
            localIps = new HashSet<>();
            localIps.addAll(getLocalInetAddress());
        }
        return localIps;
    }

    public static boolean isLocalPortOpen(int port) {
        int timeout = 1000;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(port), timeout);// 如果超时时间太长，会导致创建域的页面卡顿！！
            s.close();
            return true;
        } catch (Exception e) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(getDefaultIp(), port), timeout);// 海光专用机，startd 和 stop 会验证不通过，需要 ip 验证
                s.close();
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static List<String> getLocalInetAddress() {
        if (inetAddressList != null) {
            return inetAddressList;
        }

        inetAddressList = new ArrayList<>();

        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        List<String> third = new ArrayList<>();
        try {
            OUT:
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback()) {
                    continue;
                }
                String iName = iface.getName().toLowerCase();
                String iDisplayName = iface.getDisplayName().toLowerCase();
                String[] ignores = {"vm", "tun", "vbox", "docker", "virtual"};// *tun* 是 k8s 的 Calico 网络网卡
                for (String ignore : ignores) {
                    if (iDisplayName.contains(ignore)) {
                        continue OUT;
                    }
                }
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();

                    if (inetAddr instanceof Inet6Address) {
                        continue;// for #ITAIT-3712
                    }

                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            if (iName.startsWith("eth") || iName.startsWith("en") // ITAIT-3024
                            ) {
                                first.add(inetAddr.getHostAddress());
                            } else if (iName.startsWith("wlan")) {
                                second.add(inetAddr.getHostAddress());
                            }
                        } else {
                            third.add(inetAddr.getHostAddress());
                        }
                    }
                }
            }

            if (first.isEmpty()) {
                if (!second.isEmpty()) {
                    first.addAll(second);
                } else {
                    first.addAll(third);
                }
            }

            if (first.isEmpty()) {
                // 如果没有发现 non-loopback地址.只能用最次选的方案
                InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
                if (jdkSuppliedAddress == null) {
                    System.out.println("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
                } else {
                    first.add(jdkSuppliedAddress.getHostAddress());
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to getLocalInetAddress: " + e.getMessage());
        }

        inetAddressList = first;
        if (inetAddressList.isEmpty()) {
            inetAddressList.add("127.0.0.1");
        }
        return inetAddressList;
    }

    public static boolean pingIpPort(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(ip), port), 1000);// 如果超时时间太长，会导致创建域的页面卡顿！！
            s.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private IPUtil() {
    }
}
