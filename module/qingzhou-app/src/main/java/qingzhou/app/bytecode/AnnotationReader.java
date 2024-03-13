package qingzhou.app.bytecode;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public interface AnnotationReader {
    <A extends Annotation> A readOnClassAnnotation(Class<?> cls, Class<A> annotationClass);

    Map<Field, ModelField> readModelField(Class<?> cls);

    Map<Method, ModelAction> readModelAction(Class<?> cls);
}
