package qingzhou.command.cmd.init;

import java.io.File;

final class GlobalKeyStep {
    void init(Init init) throws Exception {
        File secretFile = init.confFile("secret-key.properties");
        String content = Init.read(secretFile);
        if (Init.hasValue(content, "global")) return; // 已有值则静默跳过

        Init.write(secretFile, Init.putValue(content, "global", init.generateCipherKey()));
        Init.restrictToOwnerOnly(secretFile);
    }
}
