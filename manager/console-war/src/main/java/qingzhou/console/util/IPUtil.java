package qingzhou.console.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;

public class IPUtil {
    private static Set<String> localIps0;
    private static volatile List<String> localIps;


    public static boolean isLocalIp(String ip) {
        if (localIps0 == null) {
            try {
                localIps0 = new HashSet<>();
                localIps0.addAll(getLocalIps());
                localIps0.add("127.0.0.1");
                localIps0.add("localhost");
                localIps0.add("::1");
                localIps0.add(InetAddress.getByName("::1").getHostAddress());
                localIps0.add("0.0.0.0");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return localIps0.contains(formatIp(ip));
    }

    public static String formatIp(String ip) {
        try {
            return InetAddress.getByName(ip).getHostAddress().split("%")[0];
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return ip;
        }
    }


    public static List<String> getLocalIps() {
        if (localIps == null || localIps.isEmpty()) {
            synchronized (IPUtil.class) {
                if (localIps == null || localIps.isEmpty()) {
                    localIps = new ArrayList<>();
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

                                if (!inetAddr.isLoopbackAddress()) {
                                    if (inetAddr.isSiteLocalAddress()) {
                                        if (iName.startsWith("eth") || iName.startsWith("en") // ITAIT-3024
                                                || iName.startsWith("br0") // br0 网卡92.13
                                        ) {
                                            first.add(inetAddr.getHostAddress());
                                        } else if (iName.startsWith("wlan")) {
                                            second.add(inetAddr.getHostAddress());
                                        }
                                    } else {
                                        third.add(inetAddr.getHostAddress().split("%")[0]);
                                    }
                                }
                            }
                        }

                        if (!second.isEmpty()) {
                            first.addAll(second);
                        } else if (!third.isEmpty()) {
                            first.addAll(third);
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

                    localIps = first;
                    if (localIps.isEmpty()) {
                        localIps.add("127.0.0.1");
                    }
                    //按找ipv4优先排序
                    localIps.sort((o1, o2) -> {
                        try {
                            int ip1 = (InetAddress.getByName(o1) instanceof Inet4Address) ? 0 : 1;
                            int ip2 = (InetAddress.getByName(o2) instanceof Inet4Address) ? 0 : 1;
                            return ip1 - ip2;
                        } catch (UnknownHostException e) {
                            return 0;
                        }
                    });
                }
            }
        }

        return localIps;
    }
}
