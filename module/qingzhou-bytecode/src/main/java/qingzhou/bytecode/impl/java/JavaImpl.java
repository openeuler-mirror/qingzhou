package qingzhou.bytecode.impl.java;

import qingzhou.bytecode.AnnotationReader;
import qingzhou.bytecode.BytecodeService;

import java.io.File;

public class JavaImpl implements BytecodeService {

    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new JavaAnnotationReaderImpl(parent);
    }
}
