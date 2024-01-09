package qingzhou.framework.bytecode.impl;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.bytecode.AnnotationReader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotationReaderImpl implements AnnotationReader {
    private final ClassLoader classLoader;

    public AnnotationReaderImpl(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Model getClassAnnotations(String classname) throws Exception {
        return classLoader.loadClass(classname).getDeclaredAnnotation(Model.class);
    }

    @Override
    public Map<String, ModelField> getFieldAnnotations(String classname) throws Exception {
        Field[] fields = classLoader.loadClass(classname).getFields();
        Map<String, ModelField> map = new LinkedHashMap<>(fields.length);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            ModelField modelField = field.getAnnotation(ModelField.class);
            if (modelField != null) {
                map.put(field.getName(), modelField);
            }
        }

        return map;
    }

    @Override
    public Map<String, ModelAction> getMethodAnnotations(String classname) throws Exception {
        Class<?> modelClass = classLoader.loadClass(classname);
        Method[] methods = modelClass.getMethods();
        Map<String, ModelAction> map = new LinkedHashMap<>(methods.length);
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            ModelAction action = method.getAnnotation(ModelAction.class);
            if (action == null) {
                action = searchActionFromParent(modelClass, method);
            }

            if (action != null) {
                map.put(method.getName(), action);
            }
        }

        return map;
    }

    private ModelAction searchActionFromParent(Class<?> thisClass, Method targetMethod) {
        if (thisClass == null) {
            return null;
        }

        for (Method m : thisClass.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
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
            ModelAction action = searchActionFromParent(superclass, targetMethod);
            if (action != null) {
                return action;
            }
        }

        for (Class<?> check : thisClass.getInterfaces()) {
            ModelAction action = searchActionFromParent(check, targetMethod);
            if (action != null) {
                return action;
            }
        }

        return null;
    }


    private boolean equalMethod(Method m, Method other) {
        if (m.getName().equals(other.getName())) {
            if (!m.getReturnType().equals(other.getReturnType())) {
                return false;
            }
            return equalParamTypes(m.getParameterTypes(), other.getParameterTypes());
        }
        return false;
    }

    private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
