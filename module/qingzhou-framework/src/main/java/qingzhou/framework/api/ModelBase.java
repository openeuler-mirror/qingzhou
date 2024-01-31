package qingzhou.framework.api;

import qingzhou.framework.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

// 所有 @Model 的实现类都要从此类继承
public abstract class ModelBase implements ShowModel {
    private AppContext appContext;

    // 由框架注入
    public final void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public final AppContext getAppContext() {
        return appContext;
    }

    // 添加 i18n 等定制初始化
    public void init() {
    }

    // 对于创建时候未传入 id 参数的，如 master 的 App，可以通过此方法计算 id，以进行必要的查重校验等操作
    public String resolveId(Request request) {
        return null;
    }

    // 定制校验逻辑，返回 i18n 的 key
    public String validate(Request request, String fieldName) {
        return null;
    }

    public Groups group() {
        return null;
    }

    public Options options(String fieldName) {
        ModelManager manager = getAppContext().getConsoleContext().getModelManager();
        String modelName = manager.getModelName(this.getClass());
        ModelField modelField = manager.getModelField(modelName, fieldName);

        if (modelField.type() == FieldType.selectCharset) {
            return Options.of(
                    Option.of("UTF-8"),
                    Option.of("GBK"),
                    Option.of("GB18030"),
                    Option.of("GB2312"),
                    Option.of("UTF-16"),
                    Option.of("US-ASCII")
            );
        }

        String refModel = modelField.refModel();
        if (StringUtil.notBlank(refModel)) {
            if (modelField.required()) {
                return refModel(refModel);
            } else {
                if (modelField.type() == FieldType.checkbox
                        || modelField.type() == FieldType.sortableCheckbox
                        || modelField.type() == FieldType.multiselect) { // 复选框，不选表示为空，不需要有空白项在页面上。
                    return refModel(refModel);
                }

                Options options = refModel(refModel);
                options.options().add(0, Option.of("", new String[0]));
                return options;
            }
        }

        if (modelField.type() == FieldType.bool) {
            return Options.of(Option.of(Boolean.TRUE.toString()), Option.of(Boolean.FALSE.toString()));
        }

        return null;// 应该再子类中实现
    }

    private Options refModel(String modelName) {
        try {
            ModelManager modelManager = getAppContext().getConsoleContext().getModelManager();
            ModelBase modelInstance = modelManager.getModelInstance(modelName);
            List<Option> options = new ArrayList<>();
            List<String> dataIdList = ((ListModel) modelInstance).getAllDataId(modelName);
            for (String dataId : dataIdList) {
                options.add(Option.of(dataId, new String[]{dataId, "en:" + dataId}));
            }
            return () -> options;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
