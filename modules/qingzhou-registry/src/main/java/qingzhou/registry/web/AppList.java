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
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/app/list",
        service = {AppList.class, HttpHandler.class})
public class AppList implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;
    @Reference
    private Json json;

    public Function<HandlingContext, Object> function = new Function<HandlingContext, Object>() {
        @Override
        public Object apply(HandlingContext context) {
            String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
            List<Map<String, String>> appBasicInfoList = new ArrayList<>();
            for (String localApp : registry.getAllLocalApps()) {
                appBasicInfoList.add(appInfo(registry.getLocalInstance(), registry.getLocalApp(localApp).getAppMeta().getApp(), lang));
            }
            registry.getAllRemoteInstances().forEach(instance -> {
                InstanceInfo remoteInstance = registry.getRemoteInstance(instance);
                registry.getAllRemoteApps(instance).forEach(appCode -> {
                    qingzhou.dto.meta.annotation.App app = registry.getRemoteApp(instance, appCode).getAppMeta().getApp();
                    appBasicInfoList.add(appInfo(remoteInstance, app, lang));
                });
            });
            return appBasicInfoList;
        }
    };

    private Map<String, String> appInfo(InstanceInfo instanceInfo, qingzhou.dto.meta.annotation.App app, String lang) {
        Map<String, String> appInfo = new HashMap<>();
        appInfo.put(WebUtil.INSTANCE_ID, instanceInfo.getId());
        appInfo.put(WebUtil.APP_CODE, app.code);
        appInfo.put("icon", app.icon);
        appInfo.put("name", i18nService.getI18n(app.name, lang));
        appInfo.put("info", i18nService.getI18n(app.info, lang));
        return appInfo;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        WebUtil.sendResult(function, httpRequest, httpResponse, registry, json);
    }
}