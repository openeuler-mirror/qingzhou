package qingzhou.app;

import qingzhou.api.*;
import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.api.type.Showable;
import qingzhou.app.bytecode.AnnotationReader;
import qingzhou.app.bytecode.impl.AnnotationReaderImpl;
import qingzhou.framework.Constants;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ModelManagerImpl implements ModelManager, Serializable {
    private Map<String, ModelInfo> modelInfoMap;

    public void initModelManager(File[] appLib, URLClassLoader loader) throws Exception {
        Map<String, ModelInfo> tempMap = new HashMap<>();
        AnnotationReader annotation = AnnotationReaderImpl.getAnnotationReader();
        Map<Method, ModelAction> presetActions = annotation.readModelAction(ActionMethod.class);
        for (File file : appLib) {
            if (!file.getName().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String entryName = jarEntry.getName();
                    String endsWithFlag = ".class";
                    if (entryName.contains("$") || !entryName.endsWith(endsWithFlag)) continue;
                    int i = entryName.indexOf(endsWithFlag);
                    String className = entryName.substring(0, i).replace("/", ".");
                    Class<?> cls = loader.loadClass(className);

                    Model model = annotation.readOnClassAnnotation(cls, Model.class);
                    if (model != null) {
                        ModelBase instance = createModelBase(cls);

                        // 1. 处理 字段
                        List<FieldInfo> fieldInfoList = getFieldInfos(annotation, cls, instance);
                        // 2. 处理 操作
                        ActionMethod actionMethod = new ActionMethod(instance);
                        Map<String, ActionInfo> actionInfoMap = getActionInfoMap(annotation, actionMethod, presetActions, cls, instance);
                        // 3. 组装 Model 数据
                        ModelInfo modelInfo = new ModelInfo(ModelUtil.toModelData(model), fieldInfoList, actionInfoMap.values(), instance);

                        ModelInfo already = tempMap.put(model.name(), modelInfo);
                        if (already != null) {
                            throw new IllegalStateException("Duplicate model name: " + model.name());
                        }
                    }
                }
            }
        }
        modelInfoMap = Collections.unmodifiableMap(tempMap);
    }

    private Map<String, ActionInfo> getActionInfoMap(AnnotationReader annotation, ActionMethod actionMethod, Map<Method, ModelAction> presetActions, Class<?> cls, ModelBase instance) {
        Map<String, ActionInfo> actionInfos = new HashMap<>();

        // 1. 添加预设的 Action
        Arrays.stream(cls.getInterfaces()).filter(aClass -> aClass.getPackage() == Showable.class.getPackage()).distinct().flatMap((Function<Class<?>, Stream<String>>) aClass -> Arrays.stream(aClass.getFields()).filter(field -> field.getName().startsWith("ACTION_NAME_")).map(field -> {
            try {
                return field.get(null).toString();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        })).forEach(actionName -> {
            for (Map.Entry<Method, ModelAction> entry : presetActions.entrySet()) {
                ModelAction modelAction = entry.getValue();
                if (modelAction.name().equals(actionName)) {
                    ActionInfo actionInfo = new ActionInfo(
                            ModelUtil.toModelActionData(modelAction),
                            actionName,
                            new InvokeMethodImpl(actionMethod, entry.getKey()));
                    actionInfos.put(actionName, actionInfo);
                    break;
                }
            }
        });

        // 2. 添加 Mode 自定义的 Action
        Map<Method, ModelAction> clsActions = annotation.readModelAction(cls);
        for (Map.Entry<Method, ModelAction> entry : clsActions.entrySet()) {
            ModelAction modelAction = entry.getValue();
            String actionName = modelAction.name();
            ActionInfo.InvokeMethod invokeMethod = new InvokeMethodImpl(instance, entry.getKey());
            ModelActionData modelActionData = ModelUtil.toModelActionData(modelAction);
            actionInfos.put(actionName, new ActionInfo(modelActionData, actionName, invokeMethod));
        }

        return actionInfos;
    }

    private static class InvokeMethodImpl implements ActionInfo.InvokeMethod {
        private final Object instance;
        private final Method method;

        private InvokeMethodImpl(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public void invoke(Object... args) throws Exception {
            method.invoke(instance, args);
        }
    }

    private List<FieldInfo> getFieldInfos(AnnotationReader annotation, Class<?> cls, ModelBase instance) throws Exception {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        for (Map.Entry<Field, ModelField> modelFieldEntry : annotation.readModelField(cls).entrySet()) {
            String defaultValue = getDefaultValue(modelFieldEntry.getKey(), instance);
            fieldInfoList.add(new FieldInfo(modelFieldEntry.getKey().getName(), ModelUtil.toModelFieldData(modelFieldEntry.getValue()), defaultValue));
        }
        return fieldInfoList;
    }

    private ModelBase createModelBase(Class<?> cls) throws Exception {
        if (!ModelBase.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("The class annotated by the @Model ( " + cls.getName() + " ) needs to 'extends ModelBase'.");
        }
        try {
            return (ModelBase) cls.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
        }
    }

    private String getDefaultValue(Field field, ModelBase modelBase) throws Exception {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }

            String fieldValue = "";
            Object fieldValObj = field.get(modelBase);
            if (fieldValObj != null) {
                fieldValue = String.valueOf(fieldValObj);
            }
            return fieldValue;
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }

    public ModelInfo getModelInfo(String modelName) {
        ModelInfo modelInfo = modelInfoMap.get(modelName);
        if (modelInfo == null) {
            throw new IllegalArgumentException("Model not found: " + modelName);
        }
        return modelInfo;
    }

    // 以下的是公开的方法

    @Override
    public String[] getModelNames() {
        return modelInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public ModelData getModel(String modelName) {
        return getModelInfo(modelName).model;
    }

    @Override
    public String[] getActionNames(String modelName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        return modelInfo.actionInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getActionNamesSupportBatch(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.values().stream()
                .filter(actionInfo -> actionInfo.modelAction.supportBatch())
                .map(actionInfo -> actionInfo.modelAction.name())
                .toArray(String[]::new);
    }

    @Override
    public ModelActionData getModelAction(String modelName, String actionName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        for (ActionInfo ai : modelInfo.actionInfoMap.values()) {
            if (ai.modelAction.name().equals(actionName)) {
                return ai.modelAction;
            }
        }

        return null;
    }

    @Override
    public Map<String, String> getModelDefaultProperties(String modelName) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, FieldInfo> entry : getModelInfo(modelName).fieldInfoMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getDefaultValue());
        }
        return result;
    }

    @Override
    public String[] getFieldNames(String modelName) {
        return getModelInfo(modelName).fieldInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public ModelFieldData getModelField(String modelName, String fieldName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        FieldInfo fieldInfo = modelInfo.fieldInfoMap.get(fieldName);
        if (fieldInfo != null) {
            return fieldInfo.modelField;
        }

        return null;
    }

    @Override
    public Options getOptions(Request request, String modelName, String fieldName) {
        Options defaultOptions = getDefaultOptions(request, modelName, fieldName);
        Options userOptions = getUserOptions(request, modelName, fieldName);
        if (defaultOptions == null) return userOptions;
        if (userOptions == null) return defaultOptions;

        List<Option> merge = new ArrayList<>(defaultOptions.options());
        merge.addAll(userOptions.options());
        return () -> merge;
    }

    private Options getUserOptions(Request request, String modelName, String fieldName) {
        return modelInfoMap.get(modelName).getInstance().options(request, fieldName);
    }

    private Options getDefaultOptions(Request request, String modelName, String fieldName) {
        ModelManager manager = this;
        ModelFieldData modelField = manager.getModelField(modelName, fieldName);

        if (modelField.type() == FieldType.selectCharset) {
            return Options.of("UTF-8", "GBK", "GB18030", "GB2312", "UTF-16", "US-ASCII");
        }

        String refModel = modelField.refModel();
        if (StringUtil.notBlank(refModel)) {
            if (modelField.required()) {
                return refModel(request.getUserName(), refModel);
            } else {
                if (modelField.type() == FieldType.checkbox
                        || modelField.type() == FieldType.sortableCheckbox
                        || modelField.type() == FieldType.multiselect) { // 复选框，不选表示为空，不需要有空白项在页面上。
                    return refModel(request.getUserName(), refModel);
                }

                Options options = refModel(request.getUserName(), refModel);
                options.options().add(0, Option.of("", new String[0]));
                return options;
            }
        }

        if (modelField.type() == FieldType.bool) {
            return Options.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        }

        return null;
    }

    private Options refModel(String userName, String modelName) {
        try {
            List<String> dataIdList = new ArrayList<>();
            if (Constants.DEFAULT_ADMINISTRATOR.equals(userName)) {
                dataIdList = modelInfoMap.get(modelName).getInstance().getAppContext().getDefaultDataStore().getAllDataId(modelName);
            } else {
                Map<String, String> data = modelInfoMap.get(modelName).getInstance().getAppContext().getDefaultDataStore().getDataById("user", userName);
                if (data != null) {
                    Stream.of(data.getOrDefault(modelName + "s", "").split(","))
                            .map(String::trim)
                            .filter(StringUtil::notBlank)
                            .forEach(dataIdList::add);
                }
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

    @Override
    public Map<String, ModelFieldData> getMonitorFieldMap(String modelName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        Map<String, ModelFieldData> map = new LinkedHashMap<>();
        for (Map.Entry<String, FieldInfo> entry : modelInfo.fieldInfoMap.entrySet()) {
            ModelFieldData modelField = entry.getValue().modelField;
            if (modelField.isMonitorField()) {
                map.put(entry.getKey(), modelField);
            }
        }
        return map;
    }

    @Override
    public String[] getGroupNames(String modelName) {
        List<String> groupNames = new ArrayList<>();
        ModelInfo modelInfo = getModelInfo(modelName);
        modelInfo.fieldInfoMap.values().forEach(fieldInfo -> groupNames.add(fieldInfo.modelField.group()));
        return groupNames.toArray(new String[0]);
    }

    @Override
    public Group getGroup(String modelName, String groupName) {
        ModelBase modelInstance = null;//TODO getModelInstance(modelName);
        Groups groups = modelInstance.groups();
        if (groups != null) {
            for (Group group : groups.groups()) {
                if (group.name().equals(groupName)) {
                    return group;
                }
            }
        }

        if ("OTHERS".equals(groupName)) {
            return Group.of("OTHERS", new String[]{"其它", "en:OTHERS"});
        }

        return null;
    }

    @Override
    public String[] getFieldNamesByGroup(String modelName, String groupName) {
        List<String> fieldNames = new ArrayList<>();

        ModelInfo modelInfo = getModelInfo(modelName);
        modelInfo.fieldInfoMap.forEach((field, fieldInfo) -> {
            if (fieldInfo.modelField.group().equals(groupName)) {
                fieldNames.add(field);
            }
        });

        return fieldNames.toArray(new String[0]);
    }
}
