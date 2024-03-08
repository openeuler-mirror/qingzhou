package qingzhou.app.bytecode;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public interface AnnotationReader {
    Model readModel(Class<?> cls);

    Map<Field, ModelField> readModelField(Class<?> cls);

    Map<Method, ModelAction> readModelAction(Class<?> cls);
}
