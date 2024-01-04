package qingzhou.bytecode;

import java.util.Map;

public interface AnnotationReader {
    Object[] getClassAnnotations(String classname) throws Exception;

    Map<String, Object[]> getFieldAnnotations(String classname) throws Exception;

    Map<String, Object[]> getMethodAnnotations(String classname) throws Exception;
}
