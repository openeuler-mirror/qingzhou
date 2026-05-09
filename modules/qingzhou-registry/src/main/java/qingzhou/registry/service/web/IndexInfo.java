package qingzhou.registry.service.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import qingzhou.llm.ToolParameter;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;
import qingzhou.registry.service.llm.BaseLlmTool;
import qingzhou.registry.service.llm.HandlingContext;

@Component(property = HttpHandler.HANDLE_PATH + "=/web/index")
public class IndexInfo extends BaseLlmTool implements HttpHandler, Tool {

    @Reference
    private Registry registry;

    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<HandlingContext, Object> function = (context) -> {
        String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
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
    };

    private Map<String, String> appBasicInfo(InstanceInfo instanceInfo, App app, String langVal) {
        Map<String, String> appBasicInfo = new HashMap<>();
        appBasicInfo.put(WebUtil.REQUEST_PARAMETER_NAME_APP_ID, WebUtil.toAppId(instanceInfo.getId(), app.code));
        appBasicInfo.put("icon", app.icon);
        appBasicInfo.put("name", i18nService.getI18n(app.name, langVal));
        appBasicInfo.put("info", i18nService.getI18n(app.info, langVal));
        appBasicInfo.put("host", instanceInfo.getHost());
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
    public String description() {
        return "该接口返回可用的应用列表。每个应用包含唯一标识、名称、图标、部署主机地址和描述信息。通过该接口可获取当前轻舟平台上所有可访问的应用概览，用于后续选择具体应用或展示应用清单。";
    }

    @Override
    public ToolParameter[] parameters() {
        return new ToolParameter[]{langParameter};
    }

    @Override
    protected Function<HandlingContext, Object> toolHandler() {
        return function;
    }
}