package qingzhou.bytecode.impl.javassist;

import qingzhou.bytecode.AnnotationReader;
import qingzhou.bytecode.BytecodeService;

import java.io.File;

public class JavassistImpl implements BytecodeService {
    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(classPath, parent);
    }
}
