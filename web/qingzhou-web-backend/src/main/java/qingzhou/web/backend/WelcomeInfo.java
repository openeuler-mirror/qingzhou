package qingzhou.web.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.registry.Registry;

public class WelcomeInfo implements WebBackendHttpServer.WebHandler {
    public static final String REQUEST_PARAMETER_NAME_APP_ID = "appId";

    private final WebBackendHttpServer.ContextHelper helper;

    public WelcomeInfo(WebBackendHttpServer.ContextHelper helper) {
        this.helper = helper;
    }

    @Override
    public Object handle() {
        List<Map<String, String>> appBasicInfoList = new ArrayList<>();
        Registry registry = helper.getRegistry();
        for (String localApp : registry.getAllLocalApps()) {
            appBasicInfoList.add(appBasicInfo(registry.getLocalInstance(), registry.getLocalApp(localApp).getAppMeta().getApp()));
        }
        registry.getAllRemoteInstances().forEach(instance -> {
            InstanceInfo remoteInstance = registry.getRemoteInstance(instance);
            registry.getAllRemoteApps(instance).forEach(appCode -> {
                App app = registry.getRemoteApp(instance, appCode).getAppMeta().getApp();
                appBasicInfoList.add(appBasicInfo(remoteInstance, app));
            });
        });
        return appBasicInfoList;
    }

    private Map<String, String> appBasicInfo(InstanceInfo instanceInfo, App app) {
        Map<String, String> appBasicInfo = new HashMap<>();
        appBasicInfo.put(REQUEST_PARAMETER_NAME_APP_ID, IdResolver.toAppId(instanceInfo.getId(), app.code));
        appBasicInfo.put("icon", app.icon);
        appBasicInfo.put("name", helper.getI18n(app.name));
        appBasicInfo.put("info", helper.getI18n(app.info));
        appBasicInfo.put("host", instanceInfo.getHost());
        return appBasicInfo;
    }
}