package qingzhou.app.bytecode.impl;

import qingzhou.app.bytecode.AnnotationReader;
import qingzhou.app.bytecode.BytecodeService;

import java.io.File;

public class BytecodeImpl implements BytecodeService {

    @Override
    public AnnotationReader createAnnotationReader(File[] classPath, ClassLoader parent) {
        return new AnnotationReaderImpl(parent);
    }
}
