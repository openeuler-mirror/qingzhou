package qingzhou.registry.service.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelActionView;
import qingzhou.dto.meta.annotation.ModelFieldView;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;
import qingzhou.registry.service.WebHttpHandler;

public class ModelMetaInfo implements WebHttpHandler.WebHandler {
    private final WebHttpHandler.ContextHelper helper;

    public ModelMetaInfo(WebHttpHandler.ContextHelper helper) {
        this.helper = helper;
    }

    @Override
    public Object handle() {
        // 条件检查
        String modelId = helper.getParameter(AppMetaInfo.REQUEST_PARAMETER_NAME_MODEL_ID);
        if (modelId == null) return null;
        String[] split = IdResolver.fromModelId(modelId);
        if (split == null) return null;

        // 查找
        Registry registry = helper.getRegistry();
        String instanceId = split[0];
        String appCode = split[1];
        String modelCode = split[2];
        AppStub appStub = registry.getAppStub(instanceId, appCode);
        if (appStub == null) return null;
        qingzhou.dto.meta.annotation.App app = appStub.getAppMeta().getApp();

        // 处理结果
        for (Model model : app.models) {
            if (model.code.equals(modelCode)) {
                return modelMetaInfo(model, instanceId, appCode);
            }
        }
        return null;
    }

    private Map<String, Object> modelMetaInfo(Model model, String instanceId, String appCode) {
        Map<String, Object> modelMetaInfo = new HashMap<>();
        modelMetaInfo.put("instanceId", instanceId);
        modelMetaInfo.put("appCode", appCode);
        modelMetaInfo.put("modelCode", model.code);
        modelMetaInfo.put("icon", model.icon);
        modelMetaInfo.put("name", helper.getI18n(model.name));
        modelMetaInfo.put("info", helper.getI18n(model.info));

        List<ModelFieldView> fieldsView = new ArrayList<>();
        model.fields.forEach(src -> {
            fieldsView.add(clone(src, ModelFieldView.class));
        });
        modelMetaInfo.put("fields", fieldsView);

        List<ModelActionView> actionsView = new ArrayList<>();
        model.actions.forEach(src -> {
            actionsView.add(clone(src, ModelActionView.class));
        });
        modelMetaInfo.put("actions", actionsView);

        return modelMetaInfo;
    }

    private <T> T clone(T from, Class<T> toClass) {
        try {
            T viewObject = toClass.newInstance();
            for (Field field : toClass.getFields()) {
                Object fromVal = field.get(from);

                // 处理 i18n，只保留当期语言的数据
                if (field.getName().equals("name") || field.getName().equals("info")) {
                    fromVal = new String[]{helper.getI18n((String[]) fromVal)};
                }

                field.set(viewObject, fromVal);
            }
            return viewObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}