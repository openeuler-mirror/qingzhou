package qingzhou.command.cmd.init;

final class AdminPasswordStep {
    void init(Init init) throws Exception {
        String props = init.readProps();
        if (Init.hasValue(props, "qingzhou-auth.password")) return; // 已有值则静默跳过
        if (init.noConfirm("Reset the admin password? [y/N] ")) return;

        String password = init.readPassword("New admin password: ");
        if (password == null) return;

        init.writeProps(Init.putValue(props, "qingzhou-auth.password", init.getCrypto().getMessageDigest().digest(password)));
        init.log("Admin password updated.");
    }
}
