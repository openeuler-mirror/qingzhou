package qingzhou.console.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class IPUtil {
    private static Set<String> localIps0;

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


    public static Set<String> getLocalIps() {
        return qingzhou.engine.util.Utils.getLocalIps();
    }
}
