package qingzhou.app;

import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.api.FieldType;
import qingzhou.api.Group;
import qingzhou.api.Groups;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Option;
import qingzhou.api.Options;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.metadata.ModelActionData;
import qingzhou.api.metadata.ModelData;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.app.bytecode.AnnotationReader;
import qingzhou.app.bytecode.impl.AnnotationReaderImpl;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModelManagerImpl implements ModelManager, Serializable {
    private Map<String, ModelInfo> modelInfoMap;

    // 以下属性是为性能缓存
    private final Map<String, Map<String, String>> modelDefaultProperties = new HashMap<>();

    public void initModelManager(File[] appLib, URLClassLoader loader, AppContext appContext) {
        try {
            parseAnnotation(appLib, loader, appContext);

            reflectInstance(loader);

            initDefaultProperties();
        } catch (Exception e) {
            throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
        }
    }

    private void initDefaultProperties() throws Exception {
        // 初始化不可变的对象
        for (String modelName : getModelNames()) {
            ModelBase modelInstance = this.modelInfoMap.get(modelName).getInstance();
            for (Map.Entry<String, FieldInfo> entry : modelInfoMap.get(modelName).fieldInfoMap.entrySet()) {
                Field field = entry.getValue().getField();
                boolean accessible = field.isAccessible();
                try {
                    if (!accessible) {
                        field.setAccessible(true);
                    }

                    String fieldValue = "";
                    Object fieldValObj = field.get(modelInstance);
                    if (fieldValObj != null) {
                        fieldValue = String.valueOf(fieldValObj);
                    }
                    Map<String, String> defaultData = modelDefaultProperties.computeIfAbsent(modelName, s -> new HashMap<>());
                    defaultData.put(entry.getKey(), fieldValue);
                } finally {
                    if (!accessible) {
                        field.setAccessible(false);
                    }
                }
            }
        }
    }

    private void reflectInstance(URLClassLoader loader) throws Exception {
        for (String modelName : getModelNames()) {
            ModelInfo modelInfo = getModelInfo(modelName);
            Class<?> modelClass = loader.loadClass(modelInfo.className);

            try {
                ModelBase instance = (ModelBase) modelClass.newInstance();
                modelInfo.setInstance(instance);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
            }

            for (FieldInfo fieldInfo : modelInfo.fieldInfoMap.values()) {
                Field field = modelClass.getField(fieldInfo.fieldName);
                fieldInfo.setField(field);
            }

            for (ActionInfo actionInfo : modelInfo.actionInfoMap.values()) {
                if (actionInfo.getJavaMethod() == null) {
                    Method method = modelClass.getMethod(actionInfo.methodName, Request.class, Response.class);
                    actionInfo.setJavaMethod(method);
                }
            }
        }
    }

    private void parseAnnotation(File[] appLib, URLClassLoader loader, AppContext appContext) throws Exception {
        Map<String, ModelInfo> tempMap = new HashMap<>();
        AnnotationReader annotation = new AnnotationReaderImpl();
        Map<String, ModelAction> actionMethodMap = annotation.readModelAction(ActionMethod.class);
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
                    Model model = annotation.readModel(cls);
                    if (model != null) {
                        if (!ModelBase.class.isAssignableFrom(cls)) {
                            throw new IllegalArgumentException("The class annotated by the @Model ( " + cls.getName() + " ) needs to 'extends ModelBase'.");
                        }
                        ActionMethod actionMethod = new ActionMethod(new ActionMethod.ActionContext() {
                            @Override
                            public AppContext getAppContext() {
                                return appContext;
                            }

                            @Override
                            public DataStore getDataStore() {
                                return appContext.getDefaultDataStore();
                            }
                        });

                        List<FieldInfo> fieldInfoList = new ArrayList<>();
                        annotation.readModelField(cls).forEach((s, field) -> fieldInfoList.add(new FieldInfo(ModelUtil.toModelFieldData(field), s)));

                        List<ActionInfo> actionInfoList = new ArrayList<>();
                        Arrays.stream(cls.getInterfaces())
                                .map(Class::getName)
                                .filter(ModelUtil.interfaceToActions::containsKey)
                                .flatMap(interfaceName -> Arrays.stream(ModelUtil.interfaceToActions.get(interfaceName)))
                                .distinct() // 防止重复添加相同的动作名
                                .forEach(actionName -> {
                                    if (actionMethodMap.containsKey(actionName)) {
                                        ModelAction modelAction = actionMethodMap.get(actionName);
                                        if (modelAction != null) {
                                            ActionInfo actionInfo = new ActionInfo(ModelUtil.toModelActionData(modelAction), actionName);
                                            actionInfoList.add(actionInfo);
                                            for (Method method : actionMethod.getClass().getMethods()) {
                                                if (method.getName().equals(actionName)) {
                                                    actionInfo.setJavaMethod(method);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                });
                        Map<String, ModelAction> clsActions = annotation.readModelAction(cls);
                        clsActions.forEach((methodName, action) -> {
                            if (!actionMethodMap.containsKey(methodName) || actionInfoList.stream().noneMatch(info -> info.methodName.equals(methodName))) {
                                actionInfoList.add(new ActionInfo(ModelUtil.toModelActionData(action), methodName));
                            } else {
                                actionInfoList.replaceAll(info -> {
                                    if (info.methodName.equals(methodName)) {
                                        return new ActionInfo(ModelUtil.toModelActionData(action), methodName);
                                    }
                                    return info;
                                });
                            }
                        });

                        ModelData modelData = ModelUtil.toModelData(model);
                        ModelInfo modelInfo = new ModelInfo(modelData, fieldInfoList, actionInfoList, actionMethod, className);

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
        return Collections.unmodifiableMap(modelDefaultProperties.getOrDefault(modelName, new HashMap<>()));
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
        Options defaultOptions = getDefaultOptions(modelName, fieldName);
        Options userOptions = null;//TODO getModelInstance(modelName).options(request, fieldName);
        if (defaultOptions == null) return userOptions;
        if (userOptions == null) return defaultOptions;

        List<Option> merge = new ArrayList<>(defaultOptions.options());
        merge.addAll(userOptions.options());
        return () -> merge;
    }

    private Options getDefaultOptions(String modelName, String fieldName) {
        ModelManager manager = this;
        ModelFieldData modelField = manager.getModelField(modelName, fieldName);

        if (modelField.type() == FieldType.selectCharset) {
            return Options.of("UTF-8", "GBK", "GB18030", "GB2312", "UTF-16", "US-ASCII");
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
            return Options.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        }

        return null;
    }

    private Options refModel(String modelName) {
        try {
            ModelBase modelInstance = null;//TODO getModelInstance(modelName);
            List<Option> options = new ArrayList<>();
            List<String> dataIdList = new ArrayList<>();//((ListModel) modelInstance).getAllDataId(modelName);
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
