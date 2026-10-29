package qingzhou.command.cmd.init;

import java.io.File;

final class GlobalKeyStep {
    void init(Init init) throws Exception {
        File secretFile = init.confFile("secret-key.properties");
        String content = Init.read(secretFile);
        String keyName = init.globalKeyName(); // 与运行时读取的属性名一致
        if (Init.hasValue(content, keyName)) return; // 已有值则静默跳过

        Init.write(secretFile, Init.putValue(content, keyName, init.getCrypto().generateSecureKey()));
        Init.restrictToOwnerOnly(secretFile);
    }
}
