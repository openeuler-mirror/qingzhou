package qingzhou.engine.util;

import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class IPUtil {
    private static Set<String> localIps;

    public static boolean isIpOrHost(String newValue) {
        if (StringUtil.isBlank(newValue)) {
            return false;
        }

        // 兼容这个情况： newValue == 0.0.0.0aa
        try {
            String hostAddress = InetAddress.getByName(newValue).getHostAddress();//ITAIT-3980 ::1简写变成完整名称
            if (hostAddress != null) {
                return true;
            }
        } catch (UnknownHostException ignored) {
        }
        return false;
    }

    public static boolean isLocalIp(String ip) {
        if (getLocalIps().contains(ip)) return true;
        if ("127.0.0.1".equals(ip)) return true;
        if ("localhost".equals(ip)) return true;
        if ("::1".equals(ip)) return true;
        try {
            if (InetAddress.getByName("::1").getHostAddress().equals(ip)) return true;
        } catch (UnknownHostException ignored) {
        }

        return false;
    }

    public static Set<String> getLocalIps() {
        if (localIps != null) {
            return localIps;
        }

        localIps = new HashSet<>();

        Set<String> first = new HashSet<>();
        Set<String> second = new HashSet<>();
        Set<String> third = new HashSet<>();
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

                    if (inetAddr != null && !inetAddr.isLoopbackAddress()) {
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
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Failed to getLocalInetAddress: " + e.getMessage());
        }

        localIps = first;
        if (localIps.isEmpty()) {
            localIps.add("127.0.0.1");
        }
        return localIps;
    }
}
