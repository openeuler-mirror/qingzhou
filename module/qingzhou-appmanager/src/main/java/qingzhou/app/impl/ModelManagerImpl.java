package qingzhou.app.impl;

import qingzhou.app.impl.bytecode.AnnotationReader;
import qingzhou.app.impl.bytecode.impl.BytecodeImpl;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Group;
import qingzhou.framework.api.Groups;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Option;
import qingzhou.framework.api.Options;
import qingzhou.framework.pattern.Visitor;
import qingzhou.framework.util.StringUtil;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
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

    public void initDefaultProperties() throws Exception {
        // 初始化不可变的对象
        for (String modelName : getModelNames()) {
            ModelBase modelInstance = getModelInstance(modelName);
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

    public void init(File[] appLib, URLClassLoader loader) throws Exception {
        Map<String, ModelInfo> tempMap = new HashMap<>();
        AnnotationReader annotation = new BytecodeImpl().createAnnotationReader(appLib, loader);
        for (File file : appLib) {
            visitClassName(file, className -> {
                Model model = annotation.getClassAnnotations(className);
                if (model == null) {
                    return false;
                }
                ModelInfo modelInfo = null;
                try {
                    modelInfo = new ModelInfo(model,
                            initModelFieldInfo(className, annotation),
                            initModelActionInfo(className, annotation),
                            className);
                } catch (Throwable e) {
                    Controller.logger.warn(e.getMessage(), e);
                }
                if (modelInfo == null) {
                    return false;
                }
                ModelInfo already = tempMap.put(model.name(), modelInfo);
                if (already != null) {
                    Controller.logger.warn("Duplicate model name: " + model.name());
                }

                return false;
            });
        }
        modelInfoMap = Collections.unmodifiableMap(tempMap);
    }

    private List<ActionInfo> initModelActionInfo(String className, AnnotationReader annotation) throws Exception {
        List<ActionInfo> actionInfoList = new ArrayList<>();
        annotation.getMethodAnnotations(className).forEach((s, action) -> actionInfoList.add(new ActionInfo(action, s)));
        return actionInfoList;
    }

    private List<FieldInfo> initModelFieldInfo(String className, AnnotationReader annotation) throws Exception {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        annotation.getFieldAnnotations(className).forEach((s, field) -> fieldInfoList.add(new FieldInfo(field, s)));
        return fieldInfoList;
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
    public Model getModel(String modelName) {
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
    public ModelAction getModelAction(String modelName, String actionName) {
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
    public String getModelName(Class<?> modelClass) {
        for (Map.Entry<String, ModelInfo> entry : modelInfoMap.entrySet()) {
            if (entry.getValue().getClazz().isAssignableFrom(modelClass)) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    public ModelField getModelField(String modelName, String fieldName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        FieldInfo fieldInfo = modelInfo.fieldInfoMap.get(fieldName);
        if (fieldInfo != null) {
            return fieldInfo.modelField;
        }

        return null;
    }

    @Override
    public Options getOptions(String modelName, String fieldName) {
        Options defaultOptions = getDefaultOptions(modelName, fieldName);
        Options userOptions = getModelInstance(modelName).options(fieldName);
        if (userOptions == null) {
            return defaultOptions;
        } else {
            return Options.merge(defaultOptions, userOptions);
        }
    }

    private Options getDefaultOptions(String modelName, String fieldName) {
        ModelManager manager = this;
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

        return null;
    }

    private Options refModel(String modelName) {
        try {
            ModelBase modelInstance = getModelInstance(modelName);
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

    @Override
    public Map<String, ModelField> getMonitorFieldMap(String modelName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        Map<String, ModelField> map = new LinkedHashMap<>();
        for (Map.Entry<String, FieldInfo> entry : modelInfo.fieldInfoMap.entrySet()) {
            ModelField modelField = entry.getValue().modelField;
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
        ModelBase modelInstance = getModelInstance(modelName);
        Groups groups = modelInstance.group();
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
    public ModelBase getModelInstance(String modelName) {
        return getModelInfo(modelName).getInstance();
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

    private static void visitClassName(File jarFile, Visitor<String> visitor) throws Exception {
        if (jarFile == null || !jarFile.getName().endsWith(".jar")) {
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String entryName = jarEntry.getName();
                String endsWithFlag = ".class";
                if (entryName.contains("$") || !entryName.endsWith(endsWithFlag)) {
                    continue;
                }
                int i = entryName.indexOf(endsWithFlag);
                String className = entryName.substring(0, i).replace("/", ".");
                try {
                    if (visitor.visitAndEnd(className)) {
                        return;
                    }
                } catch (NoClassDefFoundError e) {
                    Controller.logger.warn(e.getMessage(), e);
                }
            }
        }
    }
}
