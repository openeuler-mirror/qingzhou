package qingzhou.framework.impl.model;

import qingzhou.bytecode.AnnotationReader;
import qingzhou.bytecode.BytecodeService;
import qingzhou.framework.api.*;
import qingzhou.framework.pattern.Visitor;
import qingzhou.framework.util.ServerUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModelManagerImpl implements ModelManager {
    private Map<String, ModelInfo> modelInfoMap;

    // 以下属性是为性能缓存
    private final Map<String, Map<String, String>> modelDefaultProperties = new HashMap<>();

    public void close() throws Exception {
        modelDefaultProperties.clear();
        modelInfoMap.clear();
    }

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

    public void init(File[] appLib) throws Exception {
        Map<String, ModelInfo> tempMap = new HashMap<>();
        AnnotationReader annotation = ServerUtil.getFrameworkContext()
                .getService(BytecodeService.class).createAnnotationReader(appLib, Model.class.getClassLoader());
        for (File file : appLib) {
            visitClassName(file, className -> {
                Object[] classAnnotations = annotation.getClassAnnotations(className);
                for (Object classAnnotation : classAnnotations) {
                    if (classAnnotation instanceof Model) {
                        Model model = (Model) classAnnotation;

                        ModelInfo modelInfo;
                        try {
                            modelInfo = new ModelInfo(model,
                                    initModelFieldInfo(className, annotation),
                                    initModelActionInfo(className, annotation),
                                    className);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            continue;
                        }

                        ModelInfo already = tempMap.put(model.name(), modelInfo);
                        if (already != null) {
                            new IllegalArgumentException("Duplicate model name: " + model.name()).printStackTrace();
                        }

                        break;
                    }
                }

                return true;
            });
        }
        modelInfoMap = Collections.unmodifiableMap(tempMap);
    }

    private List<ActionInfo> initModelActionInfo(String className, AnnotationReader annotation) throws Exception {
        List<ActionInfo> actionInfoList = new ArrayList<>();
        annotation.getMethodAnnotations(className).forEach((s, objects) -> {
            for (Object object : objects) {
                if (object instanceof ModelAction) {
                    actionInfoList.add(new ActionInfo((ModelAction) object, s));
                    break;
                }
            }
        });
        return actionInfoList;
    }

    private List<FieldInfo> initModelFieldInfo(String className, AnnotationReader annotation) throws Exception {
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        annotation.getFieldAnnotations(className).forEach((s, objects) -> {
            for (Object object : objects) {
                if (object instanceof ModelField) {
                    fieldInfoList.add(new FieldInfo((ModelField) object, s));
                    break;
                }
            }
        });
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
    public Class<?> getModelClass(String modelName) {
        return getModelInfo(modelName).getClazz();
    }

    @Override
    public String[] getActionNames(String modelName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        return modelInfo.actionInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getActionNamesShowToFormBottom(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.values().stream()
                .filter(actionInfo -> actionInfo.modelAction.showToFormBottom())
                .map(actionInfo -> actionInfo.modelAction.name())
                .toArray(String[]::new);
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
    public String[] getActionNamesShowToList(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.values().stream()
                .filter(actionInfo -> actionInfo.modelAction.showToList())
                .map(actionInfo -> actionInfo.modelAction.name())
                .toArray(String[]::new);
    }

    @Override
    public String[] getActionNamesShowToListHead(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.values().stream()
                .filter(actionInfo -> actionInfo.modelAction.showToListHead())
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
        return Collections.unmodifiableMap(modelDefaultProperties.get(modelName));
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
        return getModelInstance(modelName).options(fieldName);
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
    public String getFieldName(String modelName, int fieldIndex) {
        return getFieldNames(modelName)[fieldIndex];
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
        final Group[] found = new Group[1];
        modelInstance.group().groups().stream().filter(group -> group.name().equals(groupName)).findAny().ifPresent(group -> found[0] = group);
        return found[0];
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
                        // return;
                    }
                } catch (NoClassDefFoundError e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
