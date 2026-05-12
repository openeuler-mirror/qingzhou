package qingzhou.registry.service.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelActionView;
import qingzhou.dto.meta.annotation.ModelFieldView;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Parameter;
import qingzhou.registry.AppStub;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(immediate = true, property = {HttpHandler.HANDLE_PATH + "=/web/model",
        AiTool.TOOL_DESCRIPTION + "=该接口返回某个应用模块的详细信息，用于描述如何展示和操作该模块的数据。核心内容包括：模块基本信息，包含模块代码、名称、图标、所属应用；字段定义列表，每个字段包含代码、输入类型、字段类型、是否必填、是否只读、是否唯一标识、是否在列表或表单中显示、是否可用于搜索、名称、提示信息、取值范围及各种校验规则；操作定义列表，每个操作包含代码、图标、顺序、操作名称、描述，以及该操作在列表头、列表行或批处理场景下的可用性标识。通过该接口可理解一个功能模块有哪些可用的数据字段、每个字段的填写和展示规则，以及该模块支持哪些操作，从而动态生成数据管理界面或执行相应的数据操作。"}
)
public class ModelMetaInfo implements HttpHandler, AiTool {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<Context, Object> function = (context) -> {
        // 条件检查
        String modelId = context.getParameter(AppMetaInfo.REQUEST_PARAMETER_NAME_MODEL_ID);
        if (modelId == null) return null;
        String[] split = WebUtil.fromModelId(modelId);
        if (split == null) return null;

        // 查找
        String instanceId = split[0];
        String appCode = split[1];
        String modelCode = split[2];
        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();

        // 处理结果
        for (Model model : app.models) {
            if (model.code.equals(modelCode)) {
                return modelMetaInfo(i18nService, model, instanceId, appCode);
            }
        }
        return null;
    };

    private Map<String, Object> modelMetaInfo(I18nService i18nService, Model model, String instanceId, String appCode) {
        Map<String, Object> modelMetaInfo = new HashMap<>();
        modelMetaInfo.put("instanceId", instanceId);
        modelMetaInfo.put("appCode", appCode);
        modelMetaInfo.put("modelCode", model.code);
        modelMetaInfo.put("icon", model.icon);
        modelMetaInfo.put("name", i18nService.getI18n(model.name));
        modelMetaInfo.put("info", i18nService.getI18n(model.info));

        List<ModelFieldView> fieldsView = new ArrayList<>();
        model.fields.forEach(src -> fieldsView.add(cloneView(src, ModelFieldView.class, i18nService)));
        modelMetaInfo.put("fields", fieldsView);

        List<ModelActionView> actionsView = new ArrayList<>();
        model.actions.forEach(src -> actionsView.add(cloneView(src, ModelActionView.class, i18nService)));
        modelMetaInfo.put("actions", actionsView);

        return modelMetaInfo;
    }

    private <T> T cloneView(T from, Class<T> toClass, I18nService i18nService) {
        try {
            T viewObject = toClass.newInstance();
            for (Field field : toClass.getFields()) {
                Object fromVal = field.get(from);

                // 处理 i18n，只保留当期语言的数据
                if (field.getName().equals("name") || field.getName().equals("info")) {
                    fromVal = new String[]{i18nService.getI18n((String[]) fromVal)};
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
        if (WebUtil.cached(httpRequest, httpResponse, registry)) return;

        // 执行
        httpResponse.sendFinish(WebUtil.webResult(registry, json, httpRequest, function));
    }

    @Override
    public Parameter[] parameters() {
        return new Parameter[]{
                Parameter.of(AppMetaInfo.REQUEST_PARAMETER_NAME_MODEL_ID, "指定模块的ID")
        };
    }

    @Override
    public Object invoke(Map<String, Object> argsMap) {
        Context context = name -> {
            if (argsMap != null) {
                Object val = argsMap.get(name);
                return val != null ? String.valueOf(val) : null;
            }
            return null;
        };
        return function.apply(context);
    }
}