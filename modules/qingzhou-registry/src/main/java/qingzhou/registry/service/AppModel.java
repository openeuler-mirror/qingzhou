package qingzhou.registry.service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.AiTool;
import qingzhou.ai.SkillName;
import qingzhou.api.Constants;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelActionView;
import qingzhou.dto.meta.annotation.ModelFieldView;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;
import qingzhou.registry.impl.WebUtil;

@Component(property = {HttpHandler.HANDLE_PATH + "=/app/model",
        AiTool.TOOL_SKILL_NAME + "=" + SkillName.PLATFORM_REGISTRY,

        AiTool.TOOL_DESCRIPTION + "=该接口返回特定应用的特定模块的详细信息，内容包括：模块的概要信息和模块内定义的数据字段列表和支持的操作列表。对于数据字段，可提供每个字段的数据类型、显示行为、取值校验等信息，每个操作包含代码、图标、顺序、操作名称、描述，以及该操作在列表头、列表行或批处理场景下的可用性标识。通过该接口可理解一个功能模块有哪些可用的数据字段、每个字段的填写和展示规则，以及该模块支持哪些操作，从而动态生成数据管理界面或执行相应的数据操作。",

        AiTool.PARAMETER_NAME + ".1=" + WebUtil.INSTANCE_ID,
        AiTool.PARAMETER_DESCRIPTION + ".1=应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。",

        AiTool.PARAMETER_NAME + ".2=" + WebUtil.APP_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".2=应用的唯一编码，该编码在同一个轻舟实例下不会重复。",

        AiTool.PARAMETER_NAME + ".3=" + WebUtil.MODEL_CODE,
        AiTool.PARAMETER_DESCRIPTION + ".3=模块的唯一编码，该编码在同一个应用内不会重复。"
})
public class AppModel implements HttpHandler, AiTool {
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    @Reference
    private Json json;

    private final Function<HandlingContext, Object> function = (context) -> {
        String instanceId = context.getParameter(WebUtil.INSTANCE_ID);
        String appCode = context.getParameter(WebUtil.APP_CODE);
        String modelCode = context.getParameter(WebUtil.MODEL_CODE);
        if (instanceId == null || appCode == null || modelCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;

        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();
        String lang = context.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);

        // 处理结果
        for (Model model : app.models) {
            if (model.code.equals(modelCode)) {
                return modelMetaInfo(i18nService, model, instanceId, appCode, lang);
            }
        }
        return null;
    };

    private Map<String, Object> modelMetaInfo(I18nService i18nService, Model model, String instanceId, String appCode, String lang) {
        Map<String, Object> modelMetaInfo = new HashMap<>();
        modelMetaInfo.put(WebUtil.INSTANCE_ID, instanceId);
        modelMetaInfo.put(WebUtil.APP_CODE, appCode);
        modelMetaInfo.put(WebUtil.MODEL_CODE, model.code);
        modelMetaInfo.put("icon", model.icon);
        modelMetaInfo.put("name", i18nService.getI18n(model.name, lang));
        modelMetaInfo.put("info", i18nService.getI18n(model.info, lang));

        List<ModelFieldView> fieldsView = new ArrayList<>();
        model.fields.forEach(src -> fieldsView.add(cloneView(src, ModelFieldView.class, i18nService, lang)));
        modelMetaInfo.put("fields", fieldsView);

        List<ModelActionView> actionsView = new ArrayList<>();
        model.actions.forEach(src -> actionsView.add(cloneView(src, ModelActionView.class, i18nService, lang)));
        modelMetaInfo.put("actions", actionsView);

        return modelMetaInfo;
    }

    private <T> T cloneView(T from, Class<T> toClass, I18nService i18nService, String lang) {
        try {
            T viewObject = toClass.newInstance();
            for (Field field : toClass.getFields()) {
                Object fromVal = field.get(from);

                if (field.getType() == String[].class && fromVal != null) {
                    String[] arr = (String[]) fromVal;

                    // 多值 i18n 格式：任一元素包含 "|en:" 或 "|tr:"，逐元素翻译
                    boolean isMultiValueI18n = Arrays.stream(arr).anyMatch(s -> s.contains("|en:") || s.contains("|tr:"));
                    if (isMultiValueI18n) {
                        String[] translated = new String[arr.length];
                        for (int i = 0; i < arr.length; i++) {
                            translated[i] = i18nService.getI18n(new String[]{arr[i]}, lang);
                            if (translated[i] == null) translated[i] = arr[i];
                        }
                        fromVal = translated;
                    } else if (Arrays.stream(arr).anyMatch(s -> s.startsWith("en:"))) {
                        // 单值 i18n：整体翻译为一个字符串
                        fromVal = new String[]{i18nService.getI18n(arr, lang)};
                    }
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
        WebUtil.sendResult(function, httpRequest, httpResponse, registry, json);
    }

    @Override
    public String invoke(Map<String, Object> toolArgs) throws Exception {
        HandlingContext context = name -> {
            if (toolArgs != null) {
                Object val = toolArgs.get(name);
                return val != null ? String.valueOf(val) : null;
            }
            return null;
        };
        return json.toJson(function.apply(context));
    }
}