package qingzhou.framework.impl.bytecode;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelField;

import java.util.Map;

public interface AnnotationReader {
    Model getClassAnnotations(String classname) throws Exception;

    Map<String, ModelField> getFieldAnnotations(String classname) throws Exception;

    Map<String, ModelAction> getMethodAnnotations(String classname) throws Exception;
}
