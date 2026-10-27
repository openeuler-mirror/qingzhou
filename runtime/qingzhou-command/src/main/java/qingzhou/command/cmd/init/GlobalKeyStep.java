package qingzhou.command.cmd.init;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

final class GlobalKeyStep {
    void init(Init init) throws Exception {
        File secretFile = init.confFile("secret-key.properties");
        String content = Init.read(secretFile);
        if (Init.hasValue(content, "global")) return; // 已有值则静默跳过

        Init.write(secretFile, Init.putValue(content, "global", init.getCrypto().generateCipherKey()));
        restrictToOwnerOnly(secretFile);
    }

    // 密钥文件仅属主可读写：默认 umask 下同机其它用户可读，等同泄露全平台信任根
    void restrictToOwnerOnly(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) { // 非 POSIX 平台或无权限修改
        }
    }
}
