package qingzhou.registry.service.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/web/index",
        AiTool.TOOL_DESCRIPTION + "=该接口返回可用的应用列表。每个应用包含唯一标识、名称、图标、部署主机地址和描述信息。通过该接口可获取当前轻舟平台上所有可访问的应用概览，用于后续选择具体应用或展示应用清单。"})
public class IndexInfo implements HttpHandler, AiTool {

    @Reference
    private Registry registry;

    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<Context, Object> function = new Function<Context, Object>() {
        @Override
        public Object apply(Context o) {
            List<Map<String, String>> appBasicInfoList = new ArrayList<>();
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
    };

    private Map<String, String> appBasicInfo(InstanceInfo instanceInfo, App app) {
        Map<String, String> appBasicInfo = new HashMap<>();
        appBasicInfo.put(WebUtil.INSTANCE_ID, instanceInfo.getId());
        appBasicInfo.put(WebUtil.APP_CODE, app.code);
        appBasicInfo.put("icon", app.icon);
        appBasicInfo.put("name", i18nService.getI18n(app.name));
        appBasicInfo.put("info", i18nService.getI18n(app.info));
        appBasicInfo.put("instanceHost", instanceInfo.getHost());
        return appBasicInfo;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        httpResponse.sendFinish(WebUtil.webResult(registry, json, httpRequest, function));
    }

    @Override
    public Object invoke(Map<String, Object> argsMap) {
        return function.apply(null);
    }
}