package qingzhou.bytecode.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import qingzhou.bytecode.BytecodeService;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class BytecodeServiceImpl implements BytecodeService {
    public static void main(String[] args) throws Exception {

        File file = new File("D:\\work_space\\vcs\\openeuler\\qingzhou\\package\\qingzhou\\target\\qingzhou\\qingzhou\\lib\\version\\sysapp\\common\\app-common.jar");
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            ClassReader classReader = new ClassReader(inputStream);
            classReader.accept(new My(),
                    ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);
        }
    }

    private static class My extends ClassVisitor {

        protected My() {
            super(Opcodes.ASM4);
        }
    }
}
