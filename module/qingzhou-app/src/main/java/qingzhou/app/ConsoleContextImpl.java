package qingzhou.app;

import qingzhou.api.*;
import qingzhou.serialization.ModelActionData;
import qingzhou.serialization.ModelData;
import qingzhou.serialization.ModelFieldData;

import java.io.Serializable;
import java.util.Arrays;

public class ConsoleContextImpl implements ConsoleContext, Serializable {
    private ModelManager modelManager;

    @Override
    public ModelManager getModelManager() {
        return modelManager;
    }

    public void setModelManager(ModelManager modelManager, AppMetadataImpl metadata) {
        this.modelManager = modelManager;
        initI18n(metadata);
    }

    private void initI18n(AppMetadataImpl metadata) {
        for (String modelName : modelManager.getModelNames()) {
            final ModelData model = modelManager.getModel(modelName);

            // for i18n
            metadata.addI18n("model." + modelName, model.nameI18n());
            metadata.addI18n("model.info." + modelName, model.infoI18n());

            Arrays.stream(modelManager.getFieldNames(modelName)).forEach(k -> {
                ModelFieldData v = modelManager.getModelField(modelName, k);
                metadata.addI18n("model.field." + modelName + "." + k, v.nameI18n());
                String[] info = v.infoI18n();
                if (info.length > 0) {
                    metadata.addI18n("model.field.info." + modelName + "." + k, info);
                }
            });

            for (String actionName : modelManager.getActionNames(modelName)) {
                ModelActionData modelAction = modelManager.getModelAction(modelName, actionName);
                if (modelAction != null) {// todo  disable 后 有 null 的情况?
                    metadata.addI18n("model.action." + modelName + "." + modelAction.name(), modelAction.nameI18n());
                    metadata.addI18n("model.action.info." + modelName + "." + modelAction.name(), modelAction.infoI18n());
                }
            }
        }
    }
}
