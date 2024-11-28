package qingzhou.core.config.impl;

import qingzhou.core.config.*;
import qingzhou.engine.util.FileUtil;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

class JsonFileConfig implements Config {
    private final Json json;
    private final File jsonFile;
    private final OnlineUser onlineUser = new OnlineUser();

    JsonFileConfig(Json json, File jsonFile) {
        this.json = json;
        this.jsonFile = jsonFile;
    }

    @Override
    public Core getCore() {
        return readJsonFile(reader -> json.fromJson(reader, Core.class, "module", "core"));
    }

    @Override
    public void addUser(User user) throws Exception {
        writeJson(user, true, "module", "core", "console", "user");
    }

    @Override
    public void deleteUser(String... id) throws IOException {
        deleteJson(id, "module", "core", "console", "user");
    }

    @Override
    public void setWeb(Web web) throws Exception {
        writeJson(web, false, "module", "core", "console", "web");
    }

    @Override
    public void setJmx(Jmx jmx) throws Exception {
        writeJson(jmx, false, "module", "core", "console", "jmx");
    }

    @Override
    public void setSecurity(Security security) throws Exception {
        writeJson(security, false, "module", "core", "console", "security");
    }

    @Override
    public OnlineUser getOnlineUser() {
        return onlineUser;
    }

    @Override
    public void addRole(Role role) throws Exception {
        writeJson(role, true, "module", "core", "console", "role");
    }

    @Override
    public void deleteRole(String... id) throws IOException {
        deleteJson(id, "module", "core", "console", "role");
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

    private <T> T readJsonFile(UseReaderCallback<T> callback) {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile.toPath())) {
            return callback.accept(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface UseReaderCallback<T> {
        T accept(Reader reader) throws Exception;
    }
}
