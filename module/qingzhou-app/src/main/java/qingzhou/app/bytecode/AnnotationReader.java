package qingzhou.app.bytecode;

import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;

import java.util.Map;

public interface AnnotationReader {
    Model getClassAnnotations(String classname) throws Exception;

    Map<String, ModelField> getFieldAnnotations(String classname) throws Exception;

    Map<String, ModelAction> getMethodAnnotations(String classname) throws Exception;
}
