package qingzhou.core.deployer.impl;

import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

class AnnotationReader {
    private final Class<?> clazz;

    AnnotationReader(Class<?> clazz) {
        this.clazz = clazz;
    }

    Map<Field, ModelField> readModelField() {
        Field[] fields = clazz.getFields();//.getDeclaredFields();
        Map<Field, ModelField> map = new LinkedHashMap<>(fields.length);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            ModelField modelField = field.getAnnotation(ModelField.class);
            if (modelField != null) {
                map.put(field, modelField);
            }
        }

        return map;
    }

    Map<Method, ModelAction> readModelAction() {
        Method[] methods = clazz.getMethods();
        Map<Method, ModelAction> map = new LinkedHashMap<>(methods.length);
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            ModelAction action = method.getAnnotation(ModelAction.class);
            if (action == null) {
                action = searchActionFromParent(clazz, method);
            }

            if (action != null) {
                map.put(method, action);
            }
        }

        return map;
    }

    private static ModelAction searchActionFromParent(Class<?> aClass, Method targetMethod) {
        if (aClass == null || aClass == Object.class) {
            return null;
        }

        for (Method m : aClass.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }

            if (equalMethod(m, targetMethod)) {
                ModelAction action = m.getAnnotation(ModelAction.class);
                if (action != null) {
                    return action;
                }

                break;
            }
        }

        Class<?> superclass = aClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            ModelAction action = searchActionFromParent(superclass, targetMethod);
            if (action != null) {
                return action;
            }
        }

        for (Class<?> check : aClass.getInterfaces()) {
            ModelAction action = searchActionFromParent(check, targetMethod);
            if (action != null) {
                return action;
            }
        }

        return null;
    }

    private static boolean equalMethod(Method m, Method other) {
        if (m.getName().equals(other.getName())) {
            if (!m.getReturnType().equals(other.getReturnType())) {
                return false;
            }
            return equalParamTypes(m.getParameterTypes(), other.getParameterTypes());
        }
        return false;
    }

    private static boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
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
