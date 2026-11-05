package qingzhou.command.cmd;

import java.io.Console;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import qingzhou.auth.AuthHandler;
import qingzhou.command.Processor;
import qingzhou.common.Configs;
import qingzhou.crypto.Crypto;

public class Token extends Processor {
    private static final int DEFAULT_EXPIRE_DAYS = 30;
    private static final int MAX_EXPIRE_DAYS = 365;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
    private static final DateTimeFormatter EXPIRE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Console console;
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

        console = System.console();
        if (console == null) {
            log("no interactive terminal found, run this command in a terminal");
            return;
        }

        Path configFile = Paths.get(instanceDir.getAbsolutePath(), "conf", "qingzhou.properties");
        Configs configs = new Configs(parseConfig(configFile));
        crypto = createCrypto(instanceDir, globalCipherKeyName(instanceDir));
        authHandler = new AuthHandler(crypto, configs.parseConfigAsPrefix("qingzhou-auth."));
        if (!verifyPassword()) return;

        List<String> allRoles = configs.splitList("qingzhou-auth.roles");
        String[] selected = selectRoles(allRoles.toArray(new String[0]));
        long expireAt = readExpireAt();
        String username = configs.get("qingzhou-auth.username");
        String token = createToken(username, selected, expireAt);

        log("Token (user: " + username + ", roles: " + String.join(",", selected) + ") expires at "
                + EXPIRE_TIME.format(Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault())));
        log(token);
        log("Use it in the remote client as header [Authorization: Bearer <token>].");
    }

    private boolean verifyPassword() {
        String inputUser = null;
        while (inputUser == null || inputUser.trim().isEmpty()) {
            inputUser = console.readLine("Username: ");
        }
        char[] inputPass = null;
        int i = 0;
        while (inputPass == null && i++ < 3) {
            inputPass = console.readPassword("Password: ");
        }
        try {
            boolean matched = authHandler.verifyUserPassword(inputUser, inputPass == null ? null : new String(inputPass));
            if (!matched) log("invalid username or password");
            return matched;
        } finally {
            if (inputPass != null) {
                Arrays.fill(inputPass, '\0');
            }
        }
    }

    private String[] selectRoles(String[] roles) {
        for (int i = 0; i < roles.length; i++) {
            log((i + 1) + ") " + roles[i]);
        }

        while (true) {
            String input = console.readLine("Select roles to grant (numbers, comma separated): ");
            if (input == null) throw new IllegalStateException("no input, aborted");

            Set<String> selected = new LinkedHashSet<>();
            for (String item : input.split("[,\\s]+")) {
                int index = roleIndex(roles, item);
                if (index < 0) {
                    selected.clear(); // 任一编号非法即整次重选，避免静默丢弃而少授或多授角色
                    break;
                }
                selected.add(roles[index]);
            }
            if (!selected.isEmpty()) return selected.toArray(new String[0]);
            log("invalid selection, input the numbers shown above");
        }
    }

    private int roleIndex(String[] roles, String text) {
        try {
            int index = Integer.parseInt(text.trim()) - 1;
            return index >= 0 && index < roles.length ? index : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long readExpireAt() {
        while (true) {
            String input = console.readLine("Expire in days (1-" + MAX_EXPIRE_DAYS + ", default " + DEFAULT_EXPIRE_DAYS + "): ");
            if (input == null) throw new IllegalStateException("no input, aborted");

            int days;
            try {
                String text = input.trim();
                days = text.isEmpty() ? DEFAULT_EXPIRE_DAYS : Integer.parseInt(text);
            } catch (NumberFormatException e) {
                days = 0;
            }
            if (days >= 1 && days <= MAX_EXPIRE_DAYS) return System.currentTimeMillis() + days * MILLIS_PER_DAY;
            log("invalid days, input 1-" + MAX_EXPIRE_DAYS);
        }
    }

    private String createToken(String user, String[] selectedRoles, long expireAt) throws Exception {
        StringBuilder json = new StringBuilder("{\"u\":");
        json.append("\"").append(user).append("\"");
        json.append(",\"r\":[");
        for (int i = 0; i < selectedRoles.length; i++) {
            if (i > 0) json.append(',');
            json.append("\"").append(selectedRoles[i]).append("\"");
        }
        json.append("],\"e\":").append(expireAt).append('}');
        return crypto.getGlobalCipher().encrypt(json.toString());
    }

}
