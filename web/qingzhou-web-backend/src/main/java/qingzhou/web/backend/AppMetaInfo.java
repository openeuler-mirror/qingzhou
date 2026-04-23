package qingzhou.web.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

public class AppMetaInfo implements WebBackendHttpServer.WebHandler {
    public static final String REQUEST_PARAMETER_NAME_MODEL_ID = "modelId";

    private final WebBackendHttpServer.ContextHelper helper;

    public AppMetaInfo(WebBackendHttpServer.ContextHelper helper) {
        this.helper = helper;
    }

    @Override
    public Object handle() {
        // 条件检查
        String appId = helper.getParameter(WelcomeInfo.REQUEST_PARAMETER_NAME_APP_ID);
        if (appId == null) return null;
        String[] split = IdResolver.fromAppId(appId);
        if (split == null) return null;

        // 查找
        Registry registry = helper.getRegistry();
        String instanceId = split[0];
        String appCode = split[1];
        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();

        // 处理结果
        Map<String, Object> appMetaInfo = new HashMap<>();
        appMetaInfo.put("instanceId", instanceId);
        appMetaInfo.put("appCode", app.code);
        appMetaInfo.put("icon", app.icon);
        appMetaInfo.put("name", helper.getI18n(app.name));
        appMetaInfo.put("info", helper.getI18n(app.info));

        List<Map<String, String>> models = new ArrayList<>();
        app.models.forEach(model -> {
            Map<String, String> modelBasicInfo = new HashMap<>();
            modelBasicInfo.put(REQUEST_PARAMETER_NAME_MODEL_ID, IdResolver.toModelId(instanceId, appCode, model.code));
            modelBasicInfo.put("modelCode", model.code);
            modelBasicInfo.put("icon", model.icon);
            modelBasicInfo.put("menu", model.menu);
            modelBasicInfo.put("order", model.order + "");
            modelBasicInfo.put("action", model.action);
            modelBasicInfo.put("name", helper.getI18n(model.name));
            models.add(modelBasicInfo);
        });
        appMetaInfo.put("models", models);

        List<Map<String, String>> menus = new ArrayList<>();
        app.menus.forEach(menu -> {
            Map<String, String> menuMetaInfo = new HashMap<>();
            menuMetaInfo.put("menuCode", menu.code);
            menuMetaInfo.put("icon", menu.icon);
            menuMetaInfo.put("parent", menu.parent);
            menuMetaInfo.put("order", menu.order + "");
            menuMetaInfo.put("name", helper.getI18n(menu.name));
            menus.add(menuMetaInfo);
        });
        appMetaInfo.put("menus", menus);

        return appMetaInfo;
    }
}