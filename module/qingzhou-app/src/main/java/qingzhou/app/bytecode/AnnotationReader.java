package qingzhou.app.bytecode;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.util.Map;

public interface AnnotationReader {
    Model readModel(Class<?> cls);

    Map<String, ModelField> readModelField(Class<?> cls);

    Map<String, ModelAction> readModelAction(Class<?> cls);
}
