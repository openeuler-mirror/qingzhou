package qingzhou.deployer.impl.bytecode;

import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public interface AnnotationReader {
    <T extends Annotation> T readClassAnnotation(Class<?> cls, Class<T> annotationClass);

    Map<Field, ModelField> readModelField(Class<?> cls);

    Map<Method, ModelAction> readModelAction(Class<?> cls);
}
