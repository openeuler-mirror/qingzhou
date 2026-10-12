package qingzhou.registry.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.Constants;
import qingzhou.dto.I18nService;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/app/info",
        service = {AppInfo.class, HttpHandler.class})
public class AppInfo implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;
    @Reference
    private Json json;

    public Function<HandlingContext, Object> function = (context) -> {
        String instanceId = context.getParameter(WebUtil.INSTANCE_ID);
        String appCode = context.getParameter(WebUtil.APP_CODE);
        if (instanceId == null || appCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;

        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();
        String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);

        Map<String, Object> appMetaInfo = new HashMap<>();
        appMetaInfo.put(WebUtil.INSTANCE_ID, instanceId);
        appMetaInfo.put(WebUtil.APP_CODE, app.code);
        appMetaInfo.put("icon", app.icon);
        appMetaInfo.put("name", i18nService.getI18n(app.name, lang));
        appMetaInfo.put("info", i18nService.getI18n(app.info, lang));

        List<Map<String, Object>> models = new ArrayList<>();
        app.models.forEach(model -> {
            Map<String, Object> modelBasicInfo = new HashMap<>();
            modelBasicInfo.put(WebUtil.MODEL_CODE, model.code);
            modelBasicInfo.put("icon", model.icon);
            modelBasicInfo.put("menu", model.menu);
            modelBasicInfo.put("order", model.order + "");
            modelBasicInfo.put("name", i18nService.getI18n(model.name, lang));

            List<String> actionCodes = new ArrayList<>();
            for (qingzhou.dto.meta.annotation.ModelAction action : model.actions) {
                actionCodes.add(action.code);
            }
            modelBasicInfo.put("actions", actionCodes);

            models.add(modelBasicInfo);
        });
        appMetaInfo.put("models", models);

        List<Map<String, String>> menus = new ArrayList<>();
        app.menus.forEach(menu -> {
            Map<String, String> menuMetaInfo = new HashMap<>();
            menuMetaInfo.put("code", menu.code);
            menuMetaInfo.put("icon", menu.icon);
            menuMetaInfo.put("parent", menu.parent);
            menuMetaInfo.put("order", menu.order + "");
            menuMetaInfo.put("name", i18nService.getI18n(menu.name, lang));
            menus.add(menuMetaInfo);
        });
        appMetaInfo.put("menus", menus);


        List<Map<String, String>> i18ns = new ArrayList<>();
        app.i18ns.forEach(i18n -> {
            Map<String, String> map = new HashMap<>();
            map.put("code", i18n.code);
            map.put("name", i18nService.getI18n(i18n.name, lang));
            i18ns.add(map);
        });
        appMetaInfo.put("i18ns", i18ns);

        return appMetaInfo;
    };

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        WebUtil.sendResult(function, httpRequest, httpResponse, registry, json);
    }
}