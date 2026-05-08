package qingzhou.registry.service.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Tool;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/web/index")
public class IndexInfo extends BaseLlmTool implements HttpHandler, Tool {
    static final String REQUEST_PARAMETER_NAME_CACHE_KEY = "cache_key";
    static final String REQUEST_PARAMETER_NAME_APP_ID = "appId";

    static boolean cached(HttpRequest httpRequest, HttpResponse httpResponse, Registry registry) {
        String cacheKey = httpRequest.getParameter(REQUEST_PARAMETER_NAME_CACHE_KEY);
        if (cacheKey != null) {
            try {
                long cachedTime = Long.parseLong(cacheKey);
                if (cachedTime == registry.getDataTimestamp()) {
                    httpResponse.sendFinish("{\"success\":true}");
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    static String handleWeb(Registry registry, Json json, HttpRequest httpRequest, WebHandler webHandler) throws Exception {
        Object result = webHandler.handle(httpRequest::getParameter);
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("data", result);
        metaData.put(REQUEST_PARAMETER_NAME_CACHE_KEY, registry.getDataTimestamp());
        return json.toJson(metaData);
    }

    @Reference
    private Registry registry;

    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final WebHandler webHandler = new WebHandler() {
        @Override
        public Object handle(ParameterRetriever retriever) {
            String lang = (String) retriever.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
            List<Map<String, String>> appBasicInfoList = new ArrayList<>();
            for (String localApp : registry.getAllLocalApps()) {
                appBasicInfoList.add(appBasicInfo(registry.getLocalInstance(), registry.getLocalApp(localApp).getAppMeta().getApp(), lang));
            }
            registry.getAllRemoteInstances().forEach(instance -> {
                InstanceInfo remoteInstance = registry.getRemoteInstance(instance);
                registry.getAllRemoteApps(instance).forEach(appCode -> {
                    App app = registry.getRemoteApp(instance, appCode).getAppMeta().getApp();
                    appBasicInfoList.add(appBasicInfo(remoteInstance, app, lang));
                });
            });
            return appBasicInfoList;
        }
    };

    private Map<String, String> appBasicInfo(InstanceInfo instanceInfo, App app, String langVal) {
        Map<String, String> appBasicInfo = new HashMap<>();
        appBasicInfo.put(REQUEST_PARAMETER_NAME_APP_ID, IdResolver.toAppId(instanceInfo.getId(), app.code));
        appBasicInfo.put("icon", app.icon);
        appBasicInfo.put("name", i18nService.getI18n(app.name, langVal));
        appBasicInfo.put("info", i18nService.getI18n(app.info, langVal));
        appBasicInfo.put("host", instanceInfo.getHost());
        return appBasicInfo;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (IndexInfo.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        httpResponse.sendFinish(IndexInfo.handleWeb(registry, json, httpRequest, webHandler));
    }

    @Override
    public String description() {
        return "该接口返回可用的应用列表。每个应用包含唯一标识、名称、图标、部署主机地址和描述信息。通过该接口可获取当前轻舟平台上所有可访问的应用概览，用于后续选择具体应用或展示应用清单。";
    }

    @Override
    protected WebHandler toolHandler() {
        return webHandler;
    }
}