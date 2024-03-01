package qingzhou.app;

import qingzhou.api.*;

import java.util.Arrays;

public class ConsoleContextImpl implements ConsoleContext {
    private ModelManager modelManager;

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    public void setModelManager(ModelManager modelManager, AppMetadataImpl metadata) {
        this.modelManager = modelManager;
        initI18N(metadata);
    }

    private void initI18N(AppMetadataImpl metadata) {
        for (String modelName : modelManager.getModelNames()) {
            final Model model = modelManager.getModel(modelName);

            // for i18n
            metadata.addI18N("model." + modelName, model.nameI18n());
            metadata.addI18N("model.info." + modelName, model.infoI18n());

            Arrays.stream(modelManager.getFieldNames(modelName)).forEach(k -> {
                ModelField v = modelManager.getModelField(modelName, k);
                metadata.addI18N("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    metadata.addI18N("model.field.info." + modelName + "." + k, info);
                }
            });

            for (String actionName : modelManager.getActionNames(modelName)) {
                ModelAction modelAction = modelManager.getModelAction(modelName, actionName);
                if (modelAction != null) {// todo  disable 后 有 null 的情况?
                    metadata.addI18N("model.action." + modelName + "." + modelAction.name(), modelAction.nameI18n());
                    metadata.addI18N("model.action.info." + modelName + "." + modelAction.name(), modelAction.infoI18n());
                }
            }
        }
    }
}
