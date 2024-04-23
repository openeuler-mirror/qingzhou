package qingzhou.agent.impl;

import qingzhou.agent.Agent;
import qingzhou.config.Remote;
import qingzhou.crypto.CryptoService;
import qingzhou.crypto.KeyPairCipher;
import qingzhou.registry.InstanceInfo;

import java.net.*;
import java.util.*;

public class AgentImpl implements Agent {
    private final Remote remote;
    private final CryptoService cryptoService;
    private InstanceInfo instanceInfo;
    private static Set<String> localIps;

    public AgentImpl(Remote remote, CryptoService cryptoService) {
        this.remote = remote;
        this.cryptoService = cryptoService;
    }

    @Override
    public InstanceInfo thisInstanceInfo() {
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo();
            instanceInfo.setId(UUID.randomUUID().toString().replace("-", ""));
            instanceInfo.setName(remote.getName());
            instanceInfo.setHost(remote.getHost() != null && !remote.getHost().isEmpty()
                    ? remote.getHost()
                    : Arrays.toString(getLocalIps().toArray(new String[0])));
            instanceInfo.setPort(remote.getPort());
            KeyPairCipher keyPairCipher;
            try {
                keyPairCipher = this.cryptoService.getKeyPairCipher(remote.getMaster().getPublicKey(), null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String key = keyPairCipher.encryptWithPublicKey(UUID.randomUUID().toString());
            instanceInfo.setKey(key);
        }
        return instanceInfo;
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
