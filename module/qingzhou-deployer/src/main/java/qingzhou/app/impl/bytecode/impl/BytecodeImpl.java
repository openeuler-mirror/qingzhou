package qingzhou.app.impl.bytecode.impl;

import qingzhou.app.impl.bytecode.AnnotationReader;
import qingzhou.app.impl.bytecode.BytecodeService;

import java.io.File;

public class BytecodeImpl implements BytecodeService {

    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(parent);
    }
}