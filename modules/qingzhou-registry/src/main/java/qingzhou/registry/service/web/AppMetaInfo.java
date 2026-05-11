package qingzhou.registry.service.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ParameterType;
import qingzhou.llm.Tool;
import qingzhou.llm.ToolParameter;
import qingzhou.registry.AppStub;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;
import qingzhou.registry.service.llm.BaseLlmTool;
import qingzhou.registry.service.llm.HandlingContext;

@Component(property = HttpHandler.HANDLE_PATH + "=/web/app")
public class AppMetaInfo extends BaseLlmTool implements HttpHandler, Tool {
    static final String REQUEST_PARAMETER_NAME_MODEL_ID = "modelId";

    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<HandlingContext, Object> function = (context) -> {
        // 条件检查
        String appId = context.getParameter(WebUtil.REQUEST_PARAMETER_NAME_APP_ID);
        if (appId == null) return null;
        String[] split = WebUtil.fromAppId(appId);
        if (split == null) return null;

        // 查找
        String instanceId = split[0];
        String appCode = split[1];
        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();

        String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);

        // 处理结果
        Map<String, Object> appMetaInfo = new HashMap<>();
        appMetaInfo.put("instanceId", instanceId);
        appMetaInfo.put("appCode", app.code);
        appMetaInfo.put("icon", app.icon);
        appMetaInfo.put("name", i18nService.getI18n(app.name, lang));
        appMetaInfo.put("info", i18nService.getI18n(app.info, lang));

        List<Map<String, String>> models = new ArrayList<>();
        app.models.forEach(model -> {
            Map<String, String> modelBasicInfo = new HashMap<>();
            modelBasicInfo.put(REQUEST_PARAMETER_NAME_MODEL_ID, WebUtil.toModelId(instanceId, appCode, model.code));
            modelBasicInfo.put("modelCode", model.code);
            modelBasicInfo.put("icon", model.icon);
            modelBasicInfo.put("menu", model.menu);
            modelBasicInfo.put("order", model.order + "");
            modelBasicInfo.put("action", model.action);
            modelBasicInfo.put("name", i18nService.getI18n(model.name, lang));
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
    public String description() {
        return "该接口返回指定应用的完整功能结构等详细信息。内容包括应用的基本信息（名称、图标、代码标识、描述）；应用下所有功能模块的列表，每个模块包含唯一标识、功能代码、图标、显示名称、所属菜单及排序序号；以及应用的菜单体系，包括菜单代码、父子关系、图标、名称和排序。通过该接口可理解一个应用的详细信息，如具备哪些可操作的功能模块，以及这些模块在前端菜单中的组织层级与展示顺序。";
    }

    @Override
    public ToolParameter[] parameters() {
        return new ToolParameter[]{
                langParameter,
                ToolParameter.of(WebUtil.REQUEST_PARAMETER_NAME_APP_ID, "指定应用的ID", ParameterType.STRING, true)
        };
    }

    @Override
    protected Function<HandlingContext, Object> toolHandler() {
        return function;
    }
}