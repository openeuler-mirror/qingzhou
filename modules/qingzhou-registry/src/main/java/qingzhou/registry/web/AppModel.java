package qingzhou.registry.web;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.Constants;
import qingzhou.dto.I18nService;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelActionView;
import qingzhou.dto.meta.annotation.ModelFieldView;
import qingzhou.http.server.HttpHandler;
import qingzhou.json.Json;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/app/model",
        service = {AppModel.class, HttpHandler.class})
public class AppModel extends AbstractMetaHandler {
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

    private final Function<RequestParameters, Object> function = (context) -> {
        String instanceId = context.getParameter(RequestParameters.INSTANCE_ID);
        String appCode = context.getParameter(RequestParameters.APP_CODE);
        String modelCode = context.getParameter(RequestParameters.MODEL_CODE);
        if (instanceId == null || appCode == null || modelCode == null) return null;

        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;

        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();
        String lang = context.getParameter(Constants.WEB_PARAMETER_LANG);

        // 处理结果
        for (Model model : app.models) {
            if (model.code.equals(modelCode)) {
                return modelMetaInfo(i18nService, model, instanceId, appCode, lang);
            }
        }
        return null;
    };

    public Function<RequestParameters, Object> getFunction() {
        return function;
    }

    private Map<String, Object> modelMetaInfo(I18nService i18nService, Model model, String instanceId, String appCode, String lang) {
        Map<String, Object> modelMetaInfo = new HashMap<>();
        modelMetaInfo.put(RequestParameters.INSTANCE_ID, instanceId);
        modelMetaInfo.put(RequestParameters.APP_CODE, appCode);
        modelMetaInfo.put(RequestParameters.MODEL_CODE, model.code);
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
            T viewObject = toClass.getDeclaredConstructor().newInstance(); // getDeclaredConstructor().newInstance() 为非废弃写法
            for (Field field : toClass.getFields()) {
                Object fromVal = field.get(from);

                if (field.getType() == String[].class && fromVal != null) {
                    String[] arr = (String[]) fromVal;

                    if (Arrays.stream(arr).anyMatch(s -> s.startsWith("en:"))) {
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

}