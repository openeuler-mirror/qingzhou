package qingzhou.registry.web;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.RegistryImpl;

@Component(property = HttpHandler.HANDLE_PATH + "=/register",
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Register implements HttpHandler {

    static String decryptRequest(HttpRequest httpRequest, HttpResponse httpResponse, PairCipher pairCipher) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return null;

        byte[] decrypted;
        try {
            decrypted = pairCipher.decryptWithPrivateKey(requestBody);
        } catch (Exception e) {
            httpResponse.sendFinish("key auth error");
            return null;
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    @Reference
    private Logger logger;
    @Reference
    private Crypto crypto;
    @Reference
    private Registry registry;
    @Reference
    private Json json;

    private PairCipher pairCipher;
    private Timer timer;

    @Activate
    public void start(Map<String, String> config) throws Exception {
        pairCipher = crypto.getPairCipher(null, config.get("private_key"));

        long interval = 1000 * Long.parseLong(config.get("interval"));
        long timeout = 1000 * Long.parseLong(config.get("timeout"));
        timer = new Timer("registry-health-check");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (Refresh.REFRESH_KEY_LOCK) {
                    Set<String> toRemove = new HashSet<>();
                    for (String next : registry.getAllRemoteInstances()) {
                        InstanceInfo remoteInstance = registry.getRemoteInstance(next);
                        if (remoteInstance != null) {
                            long lastRefreshTime = remoteInstance.getLastRefreshTime();
                            if (lastRefreshTime + timeout < System.currentTimeMillis()) {
                                toRemove.add(next);
                            }
                        }
                    }
                    toRemove.forEach(s -> ((RegistryImpl) registry).removeRemoteApps(s));
                }
            }
        }, interval, interval);
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        synchronized (Refresh.REFRESH_KEY_LOCK) {
            handle0(httpRequest, httpResponse);
        }
    }

    private void handle0(HttpRequest httpRequest, HttpResponse httpResponse) {
        String decryptedRequest = decryptRequest(httpRequest, httpResponse, pairCipher);
        if (decryptedRequest == null) return;

        InstanceInfo instanceInfo;
        try {
            instanceInfo = json.fromJson(decryptedRequest, InstanceInfo.class);
            instanceInfo.setHost(httpRequest.getRemoteHost());
        } catch (Exception e) {
            httpResponse.sendFinish("data format error");
            return;
        }

        RegistryImpl registryImpl = (RegistryImpl) registry;
        InstanceInfo exists = registryImpl.addRemoteApps(instanceInfo);

        String msg;
        if (exists != null) {
            msg = "Redundant.";
        } else {
            List<AppMeta> appMetas = instanceInfo.getAppMetas();
            msg = appMetas.size() + " apps accepted.";
            for (AppMeta appMeta : appMetas) {
                logger.info(String.format("registration, instance: %s, app: %s", instanceInfo.getId(), appMeta.getApp().code));
            }
        }

        String instanceKey = instanceInfo.getKey();
        byte[] encrypt;
        try {
            Cipher cipher = crypto.getCipher(instanceKey);
            encrypt = cipher.encrypt(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            httpResponse.status500Finish("instance key error");
            logger.error("encryption failed, key len: " + instanceKey.length());
            return;
        }
        httpResponse.sendFinish(encrypt);
    }
}
