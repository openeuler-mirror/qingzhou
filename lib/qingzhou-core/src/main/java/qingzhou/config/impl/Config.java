package qingzhou.config.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

import qingzhou.config.Console;
import qingzhou.config.Jmx;
import qingzhou.config.Role;
import qingzhou.config.Security;
import qingzhou.config.User;
import qingzhou.engine.util.CallbackArgs;
import qingzhou.engine.util.FileUtil;
import qingzhou.json.Json;

public class Config {
    private final Json json;
    private final File jsonFile;

    public Config(Json json, File jsonFile) {
        this.json = json;
        this.jsonFile = jsonFile;
    }

    public Console getConsole() {
        return readJsonFile(reader -> json.fromJson(reader, Console.class, "module", "core", "console"));
    }

    public void addUser(User user) throws Exception {
        writeJson(user, true, "module", "core", "console", "user");
    }

    public void deleteUser(String... id) throws IOException {
        deleteJson(id, "module", "core", "console", "user");
    }

    public void setJmx(Jmx jmx) throws Exception {
        writeJson(jmx, false, "module", "core", "console", "jmx");
    }

    public void setSecurity(Security security) throws Exception {
        writeJson(security, false, "module", "core", "console", "security");
    }

    public void addRole(Role role) throws Exception {
        writeJson(role, true, "module", "core", "console", "role");
    }

    public void deleteRole(String... id) throws IOException {
        deleteJson(id, "module", "core", "console", "role");
    }

    private <T> T readJsonFile(CallbackArgs<Reader, T> callback) {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile.toPath())) {
            return callback.callback(reader);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteJson(String[] idValues, String... position) throws IOException {
        String jsonAll = FileUtil.fileToString(jsonFile);
        String result = json.deleteJson(jsonAll,
                (Properties p) -> {
                    String checkId = p.getProperty("name");
                    for (String val : idValues) {
                        if (val.equals(checkId)) {
                            return true;
                        }
                    }
                    return false;
                },
                position);
        writeFile(result);
    }

    private void writeJson(Object obj, boolean isInArray, String... position) throws Exception {
        Properties toJson = this.json.fromJson(this.json.toJson(obj), Properties.class);

        String result;
        String json = FileUtil.fileToString(jsonFile);
        if (isInArray) {
            result = this.json.addJson(json, toJson, position);
        } else {
            result = this.json.setJson(json, toJson, position);
        }

        writeFile(result);
    }

    private void writeFile(String result) throws IOException {
        FileUtil.writeFile(jsonFile, result);
    }
}
