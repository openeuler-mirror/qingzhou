package qingzhou.bytecode.impl;

import qingzhou.bytecode.AnnotationReader;
import qingzhou.bytecode.BytecodeService;

import java.io.File;

public class BytecodeServiceImpl implements BytecodeService {
    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(classPath, parent);
    }
}
