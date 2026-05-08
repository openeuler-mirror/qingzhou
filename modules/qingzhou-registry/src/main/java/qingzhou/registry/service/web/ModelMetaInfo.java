package qingzhou.registry.service.web;

import java.lang.reflect.Field;
import java.util.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelActionView;
import qingzhou.dto.meta.annotation.ModelFieldView;
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

@Component(property = HttpHandler.HANDLE_PATH + "=/web/model")
public class ModelMetaInfo extends BaseLlmTool implements HttpHandler, Tool {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final WebHandler webHandler = (retriever) -> {
        // 条件检查
        String modelId = (String) retriever.getParameter(AppMetaInfo.REQUEST_PARAMETER_NAME_MODEL_ID);
        if (modelId == null) return null;
        String[] split = IdResolver.fromModelId(modelId);
        if (split == null) return null;

        // 查找
        String instanceId = split[0];
        String appCode = split[1];
        String modelCode = split[2];
        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();

        // 处理结果
        String lang = (String) retriever.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
        for (Model model : app.models) {
            if (model.code.equals(modelCode)) {
                return modelMetaInfo(i18nService, model, instanceId, appCode, lang);
            }
        }
        return null;
    };

    private Map<String, Object> modelMetaInfo(I18nService i18nService, Model model, String instanceId, String appCode, String langVal) {
        Map<String, Object> modelMetaInfo = new HashMap<>();
        modelMetaInfo.put("instanceId", instanceId);
        modelMetaInfo.put("appCode", appCode);
        modelMetaInfo.put("modelCode", model.code);
        modelMetaInfo.put("icon", model.icon);
        modelMetaInfo.put("name", i18nService.getI18n(model.name, langVal));
        modelMetaInfo.put("info", i18nService.getI18n(model.info, langVal));

        List<ModelFieldView> fieldsView = new ArrayList<>();
        model.fields.forEach(src -> fieldsView.add(clone(src, ModelFieldView.class, i18nService, langVal)));
        modelMetaInfo.put("fields", fieldsView);

        List<ModelActionView> actionsView = new ArrayList<>();
        model.actions.forEach(src -> actionsView.add(clone(src, ModelActionView.class, i18nService, langVal)));
        modelMetaInfo.put("actions", actionsView);

        return modelMetaInfo;
    }

    private <T> T clone(T from, Class<T> toClass, I18nService i18nService, String langVal) {
        try {
            T viewObject = toClass.newInstance();
            for (Field field : toClass.getFields()) {
                Object fromVal = field.get(from);

                // 处理 i18n，只保留当期语言的数据
                if (field.getName().equals("name") || field.getName().equals("info")) {
                    fromVal = new String[]{i18nService.getI18n((String[]) fromVal, langVal)};
                }

                field.set(viewObject, fromVal);
            }
            return viewObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return "该接口返回某个应用模块的详细信息，用于描述如何展示和操作该模块的数据。核心内容包括：模块基本信息，包含模块代码、名称、图标、所属应用；字段定义列表，每个字段包含代码、输入类型、字段类型、是否必填、是否只读、是否唯一标识、是否在列表或表单中显示、是否可用于搜索、名称、提示信息、取值范围及各种校验规则；操作定义列表，每个操作包含代码、图标、顺序、操作名称、描述，以及该操作在列表头、列表行或批处理场景下的可用性标识。通过该接口可理解一个功能模块有哪些可用的数据字段、每个字段的填写和展示规则，以及该模块支持哪些操作，从而动态生成数据管理界面或执行相应的数据操作。";
    }

    @Override
    public Set<ToolParameter> parameters() {
        Set<ToolParameter> parameters = super.parameters();
        parameters.add(ToolParameter.of(AppMetaInfo.REQUEST_PARAMETER_NAME_MODEL_ID, "指定模块的ID", ParameterType.STRING, true));
        return parameters;
    }

    @Override
    protected WebHandler toolHandler() {
        return webHandler;
    }
}