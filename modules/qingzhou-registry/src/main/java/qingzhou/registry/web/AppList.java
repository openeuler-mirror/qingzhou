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
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/app/list",
        service = {AppList.class, HttpHandler.class})
public class AppList extends AbstractMetaHandler {
    @Reference
    private I18nService i18nService;

    @Reference
    protected void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @Reference
    protected void setJson(Json json) {
        this.json = json;
    }

    private final Function<RequestParameters, Object> function = context -> {
        String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
        List<Map<String, String>> appBasicInfoList = new ArrayList<>();
        for (String localApp : registry.getAllLocalApps()) {
            qingzhou.dto.meta.annotation.App app = localAppMeta(localApp);
            if (app != null) {
                appBasicInfoList.add(appInfo(registry.getLocalInstance(), app, lang));
            }
        }
        registry.getAllRemoteInstances().forEach(instance -> {
            InstanceInfo remoteInstance = registry.getRemoteInstance(instance);
            // 实例可能被健康检查并发移除，实例信息可能为 null
            if (remoteInstance == null) return;
            // 直接从 InstanceInfo 取 meta，避免为读元数据组装重量级远程调用桩
            for (qingzhou.dto.meta.AppMeta appMeta : remoteInstance.getAppMetas()) {
                if (appMeta == null || appMeta.getApp() == null) continue;
                appBasicInfoList.add(appInfo(remoteInstance, appMeta.getApp(), lang));
            }
        });
        return appBasicInfoList;
    };

    public Function<RequestParameters, Object> getFunction() {
        return function;
    }

    private qingzhou.dto.meta.annotation.App localAppMeta(String localApp) {
        AppStub stub = registry.getLocalApp(localApp);
        return stub == null || stub.getAppMeta() == null ? null : stub.getAppMeta().getApp();
    }

    private Map<String, String> appInfo(InstanceInfo instanceInfo, qingzhou.dto.meta.annotation.App app, String lang) {
        Map<String, String> appInfo = new HashMap<>();
        appInfo.put(RequestParameters.INSTANCE_ID, instanceInfo.getId());
        appInfo.put(RequestParameters.APP_CODE, app.code);
        appInfo.put("icon", app.icon);
        appInfo.put("name", i18nService.getI18n(app.name, lang));
        appInfo.put("info", i18nService.getI18n(app.info, lang));
        return appInfo;
    }
}