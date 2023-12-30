package qingzhou.framework.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// 所有 @Model 的实现类都要从此类继承
public abstract class ModelBase implements ShowModel {
    protected AppContext appContext;

    // 由框架注入
    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    public List<String> actionsWithAjax() {
        return new ArrayList<String>() {{
            add(AddModel.ACTION_NAME_ADD);
            add(AddModel.ACTION_NAME_UPDATE);
            add(AddModel.ACTION_NAME_DELETE);
        }};
    }

    public List<String> actionsToList() {
        return new ArrayList<String>() {{
            add(DeleteModel.ACTION_NAME_DELETE);
            add(MonitorModel.ACTION_NAME_MONITOR);
            add(DownloadModel.ACTION_NAME_DOWNLOADLIST);
        }};
    }

    public List<String> actionsToListHead() {
        return new ArrayList<String>() {{
            add(AddModel.ACTION_NAME_CREATE);
        }};
    }

    public List<String> actionsSupportBatch() {
        return new ArrayList<String>() {{
            add(DeleteModel.ACTION_NAME_DELETE);
        }};
    }

    // 返回 i18n，方式：I18NRegistry.getI18N(key, args)
    public String actionNotEffective(Request request) {
        return null;
    }

    public GroupManager fieldGroups(String groupName) {
        if (groupName.equals(Group.GROUP_NAME_BASIC)) {
            return () -> new ArrayList<>(Collections.singletonList(new Group() {
                @Override
                public String name() {
                    return Group.GROUP_NAME_BASIC;
                }

                @Override
                public String[] i18n() {
                    return new String[]{"基本属性", "en:Basic"};
                }
            }));
        }
        return null;
    }

    public OptionManager fieldOptions(Request request, String fieldName) {
        ModelField modelField = getAppContext().getModelManager().getModelField(request.getModelName(), fieldName);

        if (modelField.type() == FieldType.selectCharset) {
            return () -> new ArrayList<>(Arrays.asList(
                    Option.of("UTF-8"),
                    Option.of("GBK"),
                    Option.of("GB18030"),
                    Option.of("GB2312"),
                    Option.of("UTF-16"),
                    Option.of("US-ASCII")
            ));
        }

        Class<?> refModel = modelField.refModel();
        if (refModel != Object.class) {
            if (modelField.required()) {
                return refModel(refModel);
            } else {
                if (modelField.type() == FieldType.checkbox
                        || modelField.type() == FieldType.sortableCheckbox
                        || modelField.type() == FieldType.multiselect) { // 复选框，不选表示为空，不需要有空白项在页面上。
                    return refModel(refModel);
                }
                return refModelOrEmpty(refModel);
            }
        }

        if (modelField.type() == FieldType.bool) {
            return () -> new ArrayList<>(Arrays.asList(
                    Option.of(Boolean.TRUE.toString())
                    , Option.of(Boolean.FALSE.toString())
            ));
        }

        return null;// 应该再子类中实现
    }

    private OptionManager refModelOrEmpty(Class<?> modelClass) {
        return () -> {
            List<Option> list = new ArrayList<>();
            list.add(Option.of("", new String[0]));
            OptionManager optionManager = refModel(modelClass);
            if (optionManager != null) {
                list.addAll(optionManager.options());
            }
            return list;
        };
    }

    protected OptionManager refModel(Class<?> modelClass) {
        try {
            ModelManager modelManager = getAppContext().getModelManager();
            String modelName = modelManager.getModelName(modelClass);
            ModelBase modelInstance = modelManager.getModelInstance(modelName);
            List<String> dataIdList = ((ListModel) modelInstance).getAllDataId(modelName);
            if (dataIdList == null) {
                return null;
            }
            List<Option> options = new ArrayList<>();
            for (String dataId : dataIdList) {
                options.add(Option.of(dataId, new String[]{dataId, "en:" + dataId}));
            }
            return () -> options;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isReadOnly(Request request, String fieldName) {
        return false;
    }

    // 返回 i18n，方式：I18NRegistry.getI18N(key, args)
    public String validate(Request request, String fieldName) {
        return null;
    }

    @Override
    public ModelBase clone() {
        try {
            return (ModelBase) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
