package qingzhou.bytecode.impl.java;

import qingzhou.bytecode.AnnotationReader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class JavaAnnotationReaderImpl implements AnnotationReader {
    private final ClassLoader classLoader;

    public JavaAnnotationReaderImpl(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Object[] getClassAnnotations(String classname) throws Exception {
        return classLoader.loadClass(classname).getDeclaredAnnotations();
    }

    @Override
    public Map<String, Object[]> getFieldAnnotations(String classname) throws Exception {
        Field[] fields = classLoader.loadClass(classname).getDeclaredFields();
        Map<String, Object[]> map = new LinkedHashMap<>(fields.length);
        for (Field field : fields) {
            map.put(field.getName(), field.getDeclaredAnnotations());
        }

        return map;
    }

    @Override
    public Map<String, Object[]> getMethodAnnotations(String classname) throws Exception {
        Method[] methods = classLoader.loadClass(classname).getMethods();
        Map<String, Object[]> map = new LinkedHashMap<>(methods.length);
        for (Method method : methods) {
            map.put(method.getName(), method.getDeclaredAnnotations());
        }

        return map;
    }
}
