package qingzhou.command.cmd.init;

final class AdminPasswordStep {
    void init(Init init) throws Exception {
        String props = init.readProps();
        if (Init.hasValue(props, "qingzhou-auth.password")) return; // 已有值则静默跳过

        // 无口令则实例必然启动失败，故不提供跳过项，直接要求输入
        String password = init.readPassword("New admin password: ");
        init.writeProps(Init.putValue(props, "qingzhou-auth.password", init.getCrypto().getMessageDigest().digest(password)));
        init.log("Admin password updated.");
    }
}
