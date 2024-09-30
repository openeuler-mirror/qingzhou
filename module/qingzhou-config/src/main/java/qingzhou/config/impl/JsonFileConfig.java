package qingzhou.config.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.config.Jmx;
import qingzhou.config.Security;
import qingzhou.config.User;
import qingzhou.config.Web;
import qingzhou.engine.util.FileUtil;
import qingzhou.json.Json;

public class JsonFileConfig implements Config {
    private final Json json;
    private final File jsonFile;

    public JsonFileConfig(Json json, File jsonFile) {
        this.json = json;
        this.jsonFile = jsonFile;
    }

    @Override
    public Console getConsole() {
        return readJsonFile(reader -> json.fromJson(reader, Console.class, "module", "console"));
    }

    @Override
    public void addUser(User user) throws Exception {
        writeJson(user, true, "module", "console", "user");
    }

    @Override
    public void deleteUser(String id) throws IOException {
        deleteJson("name", id, "module", "console", "user");
    }

    @Override
    public void deleteEnv(String id) throws Exception {
        deleteJson("name", id, "jvm", "env");
    }

    @Override
    public void deleteArg(String id) throws Exception {
        deleteJson("name", id, "jvm", "arg");
    }

    @Override
    public void setWeb(Web web) throws Exception {
        writeJson(web, false, "module", "console", "web");
    }

    @Override
    public void setJmx(Jmx jmx) throws Exception {
        writeJson(jmx, false, "module", "console", "jmx");
    }

    @Override
    public void setSecurity(Security security) throws Exception {
        writeJson(security, false, "module", "console", "security");
    }

    public void deleteJson(String idKey, String idVal, String... position) throws IOException {
        String jsonAll = FileUtil.fileToString(jsonFile);
        String result = json.deleteJson(jsonAll,
                p -> p.get(idKey).equals(idVal),
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
