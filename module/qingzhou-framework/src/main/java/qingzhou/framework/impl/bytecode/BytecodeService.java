package qingzhou.framework.impl.bytecode;

import java.io.File;

public interface BytecodeService {
    AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent);
}
