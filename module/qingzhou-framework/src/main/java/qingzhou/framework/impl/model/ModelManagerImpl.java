package qingzhou.framework.impl.model;

import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.Group;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Options;
import qingzhou.framework.pattern.Callback;
import qingzhou.framework.pattern.Visitor;
import qingzhou.framework.util.ClassLoaderUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModelManagerImpl implements ModelManager {
    private Map<String, ModelInfo> modelInfoMap;

    // 以下属性是为性能缓存
    private final Map<String, Map<String, String>> modelDefaultProperties = new HashMap<>();

    public ModelManagerImpl(List<File> appLib) {
        try {
            init(appLib);
            initDefaultProperties();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws Exception {
        modelDefaultProperties.clear();
        modelInfoMap.clear();
    }

    private void initDefaultProperties() throws Exception {
        // 初始化不可变的对象
        for (String modelName : getModelNames()) {
            ModelBase modelInstance = getModelInstance(modelName);
            for (Map.Entry<String, FieldInfo> entry : modelInfoMap.get(modelName).fieldInfoMap.entrySet()) {
                Field field = entry.getValue().field;
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

    private void init(List<File> appLib) throws Exception {
        URLClassLoader modelLoader = ClassLoaderUtil.newURLClassLoader(appLib, Thread.currentThread().getContextClassLoader());
        Map<String, ModelInfo> tempMap = new HashMap<>();
        visitClasses(modelLoader, clazz -> {
            if (ModelBase.class.isAssignableFrom(clazz)) {
                Model model = clazz.getAnnotation(Model.class);
                if (model != null) {
                    ModelInfo modelInfo;
                    try {
                        modelInfo = new ModelInfo(model, initModelFieldInfo(clazz), initModelActionInfo(clazz), clazz);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException(e);
                    }

                    ModelInfo already = tempMap.put(model.name(), modelInfo);
                    if (already != null) {
                        throw new IllegalArgumentException("Duplicate model name: " + model.name());
                    }
                }
            }
            return true;
        });
        modelInfoMap = Collections.unmodifiableMap(tempMap);
    }

    private void checkModelField(ModelField fieldMeta, Class<?> modelClass) {
        Class<?> refModel = fieldMeta.refModel();
        if (refModel != Object.class) {
            if (!ListModel.class.isAssignableFrom(refModel)) {
                new IllegalArgumentException("The refModel=" + refModel.getSimpleName() +
                        " referenced by " + modelClass.getSimpleName() +
                        " is not of type " + ListModel.class.getSimpleName())
                        .printStackTrace();
            }
        }
    }

    private List<FieldInfo> initModelFieldInfo(Class<?> modelClass) throws Exception {
        List<FieldInfo> fieldInfoList = initFieldInfo(modelClass, field -> {
            ModelField fieldMeta = field.getAnnotation(ModelField.class);
            if (fieldMeta != null) {
                // 是否禁用上传功能
                if (fieldMeta.type() == FieldType.file) {
                    return null;
                }

                checkModelField(fieldMeta, modelClass);

                return new FieldInfo(fieldMeta, field);
            }
            return null;
        });

        // 列表检查：需要id字段
        List<FieldInfo> result = new ArrayList<>(fieldInfoList);
        if (ListModel.class.isAssignableFrom(modelClass)) {
            if (result.stream().noneMatch(fieldInfo -> fieldInfo.field.getName().equalsIgnoreCase(ListModel.FIELD_NAME_ID))) {
                throw new IllegalArgumentException("A module [" + modelClass.getSimpleName() + "] is detected as a " + ListModel.class.getSimpleName() + " type, but the " + ListModel.FIELD_NAME_ID + " field is missing");
            }
        }

        // 列表排序：根据字段order标识进行排序，适用于如应用配置模板等有继承关系的模块
        Map<FieldInfo, Integer> map = new TreeMap<>(Comparator.comparingInt(o -> o.modelField.order()));
        for (FieldInfo fieldInfo : result) {
            if (fieldInfo.modelField.order() > -1) {
                map.put(fieldInfo, fieldInfo.modelField.order());
            }
        }
        // 首先全部移动走
        for (Map.Entry<FieldInfo, Integer> e : map.entrySet()) {
            FieldInfo fieldInfo = e.getKey();
            result.remove(fieldInfo);
        }
        // 然后再按从小到大顺序加入进来
        for (Map.Entry<FieldInfo, Integer> e : map.entrySet()) {
            FieldInfo fieldInfo = e.getKey();
            Integer order = e.getValue();
            result.add(order, fieldInfo);
        }

        return result;
    }

    private <T> List<T> initFieldInfo(Class<?> modelClass, Callback<Field, T> callback) throws Exception {
        List<T> alreadyResult = new ArrayList<>();
        Set<String> alreadyFieldNames = new HashSet<>();
        Class<?> check = modelClass;
        while (check != null) {
            List<T> thisClassResult = new ArrayList<>();
            for (Field field : check.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
                    continue;
                }

                if (alreadyFieldNames.contains(field.getName())) {
                    // 子类有了同名的，不再读取父类的
                    continue;
                }

                T run = callback.run(field);
                if (run != null) {
                    thisClassResult.add(run);
                    alreadyFieldNames.add(field.getName());
                }
            }
            thisClassResult.addAll(alreadyResult);
            alreadyResult = thisClassResult;

            check = check.getSuperclass();
        }

        return alreadyResult;
    }

    private List<ActionInfo> initModelActionInfo(Class<?> modelClass) {
        List<ActionInfo> result = new ArrayList<>();
        for (Method method : modelClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            ModelAction action = method.getAnnotation(ModelAction.class);
            if (action == null) {
                action = searchActionInfoInParent(modelClass, method);
            }

            if (action != null) {
                result.add(new ActionInfo(action, method));
            }
        }

        return result;
    }

    private ModelAction searchActionInfoInParent(Class<?> thisClass, Method targetMethod) {
        if (thisClass == null) {
            return null;
        }

        for (Method m : thisClass.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }

            if (equalMethod(m, targetMethod)) {
                ModelAction action = m.getAnnotation(ModelAction.class);
                if (action != null) {
                    return action;
                }
            }
        }

        Class<?> superclass = thisClass.getSuperclass();
        if (superclass != null) {
            ModelAction action = searchActionInfoInParent(superclass, targetMethod);
            if (action != null) {
                return action;
            }
        }

        for (Class<?> check : thisClass.getInterfaces()) {
            ModelAction action = searchActionInfoInParent(check, targetMethod);
            if (action != null) {
                return action;
            }
        }

        return null;
    }

    private boolean equalMethod(Method m, Method other) {
        if (!m.getName().equals(other.getName())) return false;

        if (!m.getReturnType().equals(other.getReturnType())) return false;

        Class<?>[] params1 = m.getParameterTypes();
        Class<?>[] params2 = other.getParameterTypes();

        if (params1.length != params2.length) return false;

        for (int i = 0; i < params1.length; i++) {
            if (params1[i] != params2[i]) {
                return false;
            }
        }

        return true;
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
        return getModelInfo(modelName).clazz;
    }

    @Override
    public String[] getActionNames(String modelName) {
        ModelInfo modelInfo = getModelInfo(modelName);
        return modelInfo.actionInfoMap.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getActionNamesShowToFormBottom(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.entrySet().stream().filter(entry -> entry.getValue().modelAction.showToFormBottom()).map(Map.Entry::getKey).toArray(String[]::new);
    }

    @Override
    public String[] getActionNamesSupportBatch(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.entrySet().stream().filter(entry -> entry.getValue().modelAction.supportBatch()).map(Map.Entry::getKey).toArray(String[]::new);
    }

    @Override
    public String[] getActionNamesShowToList(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.entrySet().stream().filter(entry -> entry.getValue().modelAction.showToList()).map(Map.Entry::getKey).toArray(String[]::new);
    }

    @Override
    public String[] getActionNamesShowToListHead(String modelName) {
        Map<String, ActionInfo> actionInfoMap = getModelInfo(modelName).actionInfoMap;
        return actionInfoMap.entrySet().stream().filter(entry -> entry.getValue().modelAction.showToListHead()).map(Map.Entry::getKey).toArray(String[]::new);
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
            if (entry.getValue().clazz.isAssignableFrom(modelClass)) {
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
        return getModelInfo(modelName).instance;
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

    private static void visitClasses(URLClassLoader classLoader, Visitor<Class<?>> visitor) throws Exception {
        for (URL url : classLoader.getURLs()) {
            String jarFilePath = url.getPath();
            if (jarFilePath.endsWith(".jar") || jarFilePath.endsWith(".jar!/") || jarFilePath.endsWith(".jar/")) {
                jarFilePath = getJarFilePath(jarFilePath);
                try (JarFile jarFile = new JarFile(jarFilePath)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
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
                            Class<?> clazz = classLoader.loadClass(className);
                            if (!visitor.visit(clazz)) {
                                return;
                            }
                        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
                            // todo 原因？
                        }
                    }
                }
            }
        }
    }

    private static String getJarFilePath(String jarFilePath) {
        int subStartIndex = 0;
        if (jarFilePath.startsWith("file:")) {
            subStartIndex = 5;
        } else if (jarFilePath.startsWith("jar:file:")) {
            subStartIndex = 9;
        }

        int subEndIndex = jarFilePath.lastIndexOf('!');
        if (subEndIndex == -1) {
            subEndIndex = jarFilePath.length();
        }

        jarFilePath = jarFilePath.substring(subStartIndex, subEndIndex);

        return jarFilePath;
    }
}
