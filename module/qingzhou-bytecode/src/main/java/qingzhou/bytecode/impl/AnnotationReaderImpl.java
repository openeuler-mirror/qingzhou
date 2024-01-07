package qingzhou.bytecode.impl;

import javassist.ClassPool;
import javassist.CtField;
import javassist.CtMethod;
import qingzhou.bytecode.AnnotationReader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotationReaderImpl implements AnnotationReader {
    private final File[] classPath;
    private final ClassLoader parent;
    private ClassPool classPool;

    public AnnotationReaderImpl(File[] classPath, ClassLoader parent) {
        this.classPath = classPath;
        this.parent = parent;
    }

    @Override
    public Object[] getClassAnnotations(String classname) throws Exception {
        return runInEnv(() -> classPool.get(classname).getAnnotations());
    }

    @Override
    public Map<String, Object[]> getFieldAnnotations(String classname) throws Exception {
        return runInEnv(() -> {
            CtField[] fields = classPool.get(classname).getFields();
            Map<String, Object[]> map = new LinkedHashMap<>(fields.length);
            for (CtField field : fields) {
                map.put(field.getName(), field.getAnnotations());
            }
            return map;
        });
    }

    @Override
    public Map<String, Object[]> getMethodAnnotations(String classname) throws Exception {
        return runInEnv(() -> {
            CtMethod[] methods = classPool.get(classname).getMethods();
            Map<String, Object[]> map = new LinkedHashMap<>(methods.length);
            for (CtMethod method : methods) {
                map.put(method.getName(), method.getAnnotations());
            }
            return map;
        });
    }

    private <T> T runInEnv(Callback<T> callback) throws Exception {
        if (classPool == null) {
            classPool = ClassPool.getDefault();
            if (classPath != null) {
                for (File file : classPath) {
                    classPool.appendClassPath(file.getCanonicalPath());
                }
            }
        }

        if (parent == null) {
            return callback.run();
        } else {
            ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(parent);
                return callback.run();
            } finally {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
        }
    }

    public interface Callback<T> {
        T run() throws Exception;
    }
}
