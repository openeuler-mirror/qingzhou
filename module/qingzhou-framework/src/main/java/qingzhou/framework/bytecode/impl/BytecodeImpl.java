package qingzhou.framework.bytecode.impl;

import qingzhou.framework.bytecode.AnnotationReader;
import qingzhou.framework.bytecode.BytecodeService;

import java.io.File;

public class BytecodeImpl implements BytecodeService {

    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(parent);
    }
}
