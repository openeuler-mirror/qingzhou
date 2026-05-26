package qingzhou.registry.service.web;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.api.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.WebUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component(property = {HttpHandler.HANDLE_PATH + "=/web/app",
        AiTool.TOOL_DESCRIPTION + "=该接口返回指定应用的完整功能结构等详细信息。内容包括应用的基本信息（名称、图标、代码标识、描述）；应用下所有功能模块的列表，每个模块包含唯一标识、功能代码、图标、显示名称、所属菜单及排序序号；以及应用的菜单体系，包括菜单代码、父子关系、图标、名称和排序。通过该接口可理解一个应用的详细信息，如具备哪些可操作的功能模块，以及这些模块在前端菜单中的组织层级与展示顺序。",

        AiTool.PARAMETER_NAME + ".0=" + Constants.REQUEST_PARAMETER_NAME_LANG,
        AiTool.PARAMETER_DESCRIPTION + ".0=指定以哪种国际化语言展示结果，简体请输入：zh，繁體请输入：tr，英语请输入：en。",
        AiTool.PARAMETER_REQUIRED + ".0=false",

        AiTool.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        AiTool.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。",

        AiTool.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".2=应用的唯一编码，该编码在同一个轻舟实例下不会重复。"
})
public class AppMetaInfo implements HttpHandler, AiTool {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<Context, Object> function = (context) -> {
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
            menuMetaInfo.put("menuCode", menu.code);
            menuMetaInfo.put("icon", menu.icon);
            menuMetaInfo.put("parent", menu.parent);
            menuMetaInfo.put("order", menu.order + "");
            menuMetaInfo.put("name", i18nService.getI18n(menu.name, lang));
            menus.add(menuMetaInfo);
        });
        appMetaInfo.put("menus", menus);

        return appMetaInfo;
    };

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 是否已缓存
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        httpResponse.sendFinish(WebUtil.webResult(registry, json, httpRequest, function));
    }

    @Override
    public Object invoke(Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        Context context = name -> {
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
        return function.apply(context);
    }
}