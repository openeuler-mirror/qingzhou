package qingzhou.framework.impl.bytecode.impl;

import qingzhou.framework.impl.bytecode.AnnotationReader;
import qingzhou.framework.impl.bytecode.BytecodeService;

import java.io.File;

public class BytecodeImpl implements BytecodeService {

    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(parent);
    }
}
