package qingzhou.agent.embedded.heartbeat;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.HttpResult;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.EmbeddedRegistry;

public class HeartbeatService {
    private final int port;
    private final String registryUrl;
    private final String publicKey;
    private final int interval;
    private final HttpClient httpClient;
    private final Crypto crypto;
    private final Registry registry;
    private final Json json;
    private final Logger logger;
    private final File instanceDir;

    private PairCipher pairCipher;
    private String refreshUrl;
    private String registerUrl;
    private Timer timer;
    private volatile InstanceInfo thisInstanceInfo;

    public HeartbeatService(int port, String registryUrl, String publicKey,
            int interval, HttpClient httpClient, Crypto crypto,
            Registry registry, Json json, Logger logger, File instanceDir) {
        this.port = port;
        this.registryUrl = registryUrl;
        this.publicKey = publicKey;
        this.interval = interval;
        this.httpClient = httpClient;
        this.crypto = crypto;
        this.registry = registry;
        this.json = json;
        this.logger = logger;
        this.instanceDir = instanceDir;
    }

    public void start() throws Exception {
        pairCipher = crypto.getPairCipher(publicKey, null);

        String url = registryUrl;
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        refreshUrl = url + "/registry/refresh";
        registerUrl = url + "/registry/register";

        buildInstanceInfo();

        timer = new Timer("agent-heartbeat", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                heartbeat();
            }
        }, 10000, interval * 1000L);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public InstanceInfo getInstanceInfo() {
        return thisInstanceInfo;
    }

    private void heartbeat() {
        try {
            if (appChanged()) {
                buildInstanceInfo();
            }

            String newKey = crypto.generateKey();
            String refreshInfos = thisInstanceInfo.getId() + "," + newKey;
            String refreshed = send(refreshUrl, refreshInfos.getBytes(StandardCharsets.UTF_8));
            if (Boolean.parseBoolean(refreshed)) {
                thisInstanceInfo.setKey(newKey);
            } else {
                String registerData = json.toJson(thisInstanceInfo);
                String registration = send(registerUrl, registerData.getBytes(StandardCharsets.UTF_8));
                logger.info("registration response: " + registration);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String send(String url, byte[] data) {
        try {
            byte[] encrypted = pairCipher.encryptWithPublicKey(data);
            HttpResult response = httpClient.request(url, HttpMethod.POST, encrypted, null);
            if (response.getStatus() != 200) {
                logger.warn("response code error [ " + response.getStatus() + " ] : " + url);
            }
            byte[] responseBody = response.getBody();

            if (responseBody == null || responseBody.length == 0) return "";

            Cipher cipher = crypto.getCipher(thisInstanceInfo.getKey());
            byte[] result = cipher.decrypt(responseBody);
            return new String(result, StandardCharsets.UTF_8);
        } catch (java.net.ConnectException e) {
            logger.error("failed to connect to the URL: " + url + ", msg: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private void buildInstanceInfo() throws Exception {
        InstanceInfo info = new InstanceInfo();

        for (String appCode : registry.getAllLocalApps()) {
            info.getAppMetas().add(registry.getLocalApp(appCode).getAppMeta());
        }
        info.getAppMetas().sort(Comparator.comparing(a -> a.getApp().code));

        String version = "1.0.0";
        info.setVersion(version);
        info.setPort(port);

        String digestTemp = json.toJson(info);
        digestTemp += ips();
        String uniqueId = crypto.getMessageDigest().md5(digestTemp);
        info.setId(uniqueId);

        info.setKey(crypto.generateKey());

        this.thisInstanceInfo = info;
        ((EmbeddedRegistry) registry).setLocalInstance(info);
    }

    private boolean appChanged() {
        InstanceInfo info = thisInstanceInfo;
        if (info == null) return true;

        List<AppMeta> appMetaList = info.getAppMetas();
        List<AppMeta> currentAppMetas = new ArrayList<>();
        for (String appCode : registry.getAllLocalApps()) {
            currentAppMetas.add(registry.getLocalApp(appCode).getAppMeta());
        }

        if (appMetaList.size() != currentAppMetas.size()) return true;

        appMetaList.sort(Comparator.comparing(o -> o.getApp().code));
        currentAppMetas.sort(Comparator.comparing(o -> o.getApp().code));

        for (int i = 0; i < appMetaList.size(); i++) {
            if (appMetaList.get(i) != currentAppMetas.get(i)) return true;
        }
        return false;
    }

    private String ips() throws Exception {
        List<String> ipList = new ArrayList<>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) continue;
                ipList.add(inetAddress.getHostAddress());
            }
        }
        Collections.sort(ipList);

        StringBuilder allIps = new StringBuilder();
        ipList.forEach(allIps::append);
        return allIps.toString();
    }
}