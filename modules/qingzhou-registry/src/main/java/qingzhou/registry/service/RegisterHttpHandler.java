package qingzhou.registry.service;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.PairCipher;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.http.server.HttpHandler;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.RegistryImpl;

@Component(property = HttpHandler.HANDLE_PATH + "=/register",
        configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RegisterHttpHandler implements HttpHandler {
    public static final Object REGISTRY_LOCK = new Object();

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

        timer = new Timer("registry-health-check");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (REGISTRY_LOCK) {
                    Set<String> toRemove = new HashSet<>();
                    long deadline = System.currentTimeMillis() - 1000 * Long.parseLong(config.get("timeout"));
                    for (String next : registry.getAllRemoteInstances()) {
                        InstanceInfo remoteInstance = registry.getRemoteInstance(next);
                        if (remoteInstance != null) {
                            long lastRefreshTime = remoteInstance.getLastRefreshTime();
                            if (lastRefreshTime > 1) {
                                if (lastRefreshTime < deadline) {
                                    toRemove.add(next);
                                }
                            }
                        }
                    }
                    toRemove.forEach(s -> ((RegistryImpl) registry).removeRemoteApps(s));
                }
            }
        }, 2000, 1000 * Long.parseLong(config.get("interval")));
    }

    @Deactivate
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        synchronized (REGISTRY_LOCK) {
            handle0(httpRequest, httpResponse);
        }
    }

    private void handle0(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
        if (requestBody.length == 0) return;

        byte[] decrypted;
        try {
            decrypted = pairCipher.decryptWithPrivateKey(requestBody);
        } catch (Exception e) {
            httpResponse.sendResponse("Key auth error !!!");
            return;
        }

        String jsonContent = new String(decrypted, StandardCharsets.UTF_8);
        InstanceInfo instanceInfo;
        try {
            instanceInfo = json.fromJson(jsonContent, InstanceInfo.class);
            instanceInfo.setHost(httpRequest.getRemoteHost());
        } catch (Exception e) {
            httpResponse.sendResponse("Data format error !!!");
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
                logger.info(String.format("App Registration, instance: %s, app: %s", instanceInfo.getId(), appMeta.getApp().code));
            }
        }

        String instanceKey = instanceInfo.getKey();
        byte[] encrypt;
        try {
            Cipher cipher = crypto.getCipher(instanceKey);
            encrypt = cipher.encrypt(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            httpResponse.statusError()
                    .sendResponse("Instance Key error !!!");
            logger.error("Encryption failed, key length: " + instanceKey.length());
            return;
        }
        httpResponse.sendResponse(encrypt);
    }
}
