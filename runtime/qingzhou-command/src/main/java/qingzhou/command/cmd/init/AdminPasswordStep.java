package qingzhou.command.cmd.init;

final class AdminPasswordStep {
    void init(Init init) throws Exception {
        String props = init.readProps();
        // 只初始化声明了的项：键被注释或删除表示该实例不启用登录（如仅作 agent），与 EnvCheck 同源判定
        if (!Init.hasKey(props, "qingzhou-auth.password")) return;
        if (Init.hasValue(props, "qingzhou-auth.password")) return; // 已有值则静默跳过

        // 键存在即视为启用登录，无口令则登录不可用，故不提供跳过项，直接要求输入
        String password = init.readPassword("Input admin password: ");
        init.writeProps(Init.putValue(props, "qingzhou-auth.password", init.getCrypto().getMessageDigest().digest(password)));
        init.log("Admin password updated.");
    }
}
