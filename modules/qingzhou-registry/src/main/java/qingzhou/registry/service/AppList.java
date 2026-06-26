package qingzhou.registry.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SystemAiTool;
import qingzhou.api.Constants;
import qingzhou.dto.I18nService;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.WebUtil;

@Component(property = {HttpHandler.HANDLE_PATH + "=/app/list",
        AiTool.TOOL_DESCRIPTION + "=该接口返回已注册的应用列表信息。每个应用包含唯一标识、名称、描述、所属实例等信息。"})
public class AppList implements HttpHandler, SystemAiTool {
    @Reference
    private Registry registry;

    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<HandlingContext, Object> function = new Function<HandlingContext, Object>() {
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

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        HandlingContext context = name -> {
            if (toolArgs == null) return null;
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
        return json.toJson(function.apply(context));
    }
}