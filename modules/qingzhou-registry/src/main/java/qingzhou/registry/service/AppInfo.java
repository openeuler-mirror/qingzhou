package qingzhou.registry.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SkillName;
import qingzhou.api.Constants;
import qingzhou.dto.I18nService;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.WebUtil;

@Component(property = {HttpHandler.HANDLE_PATH + "=/app/info",
        AiTool.TOOL_SKILL_NAME + "=" + SkillName.PLATFORM_REGISTRY,

        AiTool.TOOL_DESCRIPTION + "=该接口返回特定应用的详细信息，内容包括：应用的基本信息（代码标识、名称、描述等等）；应用内包含的业务模块列表信息（模块的代码标识、名称、描述、所属功能菜单等）。",

        AiTool.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        AiTool.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。",

        AiTool.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".2=应用的唯一编码，该编码在同一个轻舟实例下不会重复。"
})
public class AppInfo implements HttpHandler, AiTool {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<HandlingContext, Object> function = (context) -> {
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

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        if (toolArgs == null) return null;
        HandlingContext context = name -> {
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
        return json.toJson(function.apply(context));
    }
}