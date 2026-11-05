package qingzhou.command.cmd;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import qingzhou.auth.AuthHandler;
import qingzhou.command.ConsoleInput;
import qingzhou.command.Processor;
import qingzhou.common.Configs;
import qingzhou.crypto.Crypto;

public class Token extends Processor {
    private static final int DEFAULT_EXPIRE_DAYS = 30;
    private static final int MAX_EXPIRE_DAYS = 365;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
    private static final DateTimeFormatter EXPIRE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuthHandler authHandler;
    private Crypto crypto;

    @Override
    public String name() {
        return "token";
    }

    @Override
    public String info() {
        return "Create a token to access the system. Usage: token <instance>";
    }

    @Override
    public String[] supportedArgs() {
        return new String[]{"<instance>"};
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        File instanceDir = initInstance(args);
        if (instanceDir == null) return;

        Configs configs = new Configs(parseConfig(instanceDir.toPath().resolve("conf/qingzhou.properties")));
        crypto = createCrypto(instanceDir, globalCipherKeyName(instanceDir));
        authHandler = new AuthHandler(crypto, configs.parseConfigAsPrefix("qingzhou-auth."));
        if (!verifyPassword()) return;

        String[] roles = selectRoles(configs.splitList("qingzhou-auth.roles").toArray(new String[0]));
        long expireAt = readExpireAt();
        String user = configs.get("qingzhou-auth.username");
        log("Token (user: " + user + ", roles: " + String.join(",", roles) + ") expires at "
                + EXPIRE_TIME.format(Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault())));
        log(createToken(user, roles, expireAt) + "\nUse it in the remote client as header [Authorization: Bearer <token>].");
    }

    private boolean verifyPassword() {
        String user = ConsoleInput.readLine("Username: ");
        char[] pass = ConsoleInput.readPassword("Password: ");
        try {
            boolean matched = authHandler.verifyUserPassword(user, pass == null ? null : new String(pass));
            if (!matched) log("invalid username or password");
            return matched;
        } finally {
            if (pass != null) Arrays.fill(pass, '\0');
        }
    }

    private String[] selectRoles(String[] roles) {
        for (int i = 0; i < roles.length; i++) {
            log((i + 1) + ") " + roles[i]);
        }

        while (true) {
            String input = ConsoleInput.readLine("Select roles to grant (numbers, comma separated): ");
            if (input == null) throw new IllegalStateException("no input, aborted");

            Set<String> selected = new LinkedHashSet<>();
            for (String item : input.split("[,\\s]+")) {
                int index;
                try {
                    index = Integer.parseInt(item) - 1;
                } catch (NumberFormatException e) {
                    index = -1;
                }
                if (index < 0 || index >= roles.length) { // 任一编号非法即整次重选，避免静默丢弃而少授或多授角色
                    selected.clear();
                    break;
                }
                selected.add(roles[index]);
            }
            if (!selected.isEmpty()) return selected.toArray(new String[0]);
            log("invalid selection, input the numbers shown above");
        }
    }

    private long readExpireAt() {
        while (true) {
            String input = ConsoleInput.readLine("Expire in days (1-" + MAX_EXPIRE_DAYS + ", default " + DEFAULT_EXPIRE_DAYS + "): ");
            if (input == null) throw new IllegalStateException("no input, aborted");

            String text = input.trim();
            int days;
            try {
                days = text.isEmpty() ? DEFAULT_EXPIRE_DAYS : Integer.parseInt(text);
            } catch (NumberFormatException e) {
                days = 0;
            }
            if (days >= 1 && days <= MAX_EXPIRE_DAYS) return System.currentTimeMillis() + days * MILLIS_PER_DAY;
            log("invalid days, input 1-" + MAX_EXPIRE_DAYS);
        }
    }

    private String createToken(String user, String[] roles, long expireAt) throws Exception {
        String roleJson = roles.length == 0 ? "[]" : "[\"" + String.join("\",\"", roles) + "\"]";
        return crypto.getGlobalCipher().encrypt("{\"u\":\"" + user + "\",\"r\":" + roleJson + ",\"e\":" + expireAt + "}");
    }
}
