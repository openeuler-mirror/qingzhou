package qingzhou.config.impl;

import qingzhou.config.Agent;
import qingzhou.config.Arg;
import qingzhou.config.Config;
import qingzhou.config.Console;
import qingzhou.config.Env;
import qingzhou.config.Heartbeat;
import qingzhou.config.Jmx;
import qingzhou.config.Jvm;
import qingzhou.config.User;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Properties;

public class JsonFileConfig implements Config {
    private final Json json;
    private final File jsonFile;

    public JsonFileConfig(Json json, File jsonFile) {
        this.json = json;
        this.jsonFile = jsonFile;
    }

    @Override
    public File getConfigFile() {
        return jsonFile;
    }

    @Override
    public Console getConsole() {
        return readJsonFile(reader -> json.fromJson(reader, Console.class, "module", "console"));
    }

    @Override
    public Agent getAgent() {
        return readJsonFile(reader -> json.fromJson(reader, Agent.class, "module", "agent"));
    }

    @Override
    public Heartbeat getHeartbeat() {
        return readJsonFile(reader -> json.fromJson(reader, Heartbeat.class, "module", "heartbeat"));
    }

    @Override
    public void addUser(User user) throws Exception {
        newJson(user, true, "module", "console", "user");
    }

    @Override
    public void deleteUser(User user) throws IOException {
        String jsonAll = Utils.read(jsonFile);
        String result = json.deleteJson(jsonAll,
                p -> p.getProperty("id").equals(user.getId()),
                "module", "console", "user");
        Utils.writeFile(jsonFile, result);
    }

    @Override
    public void setJmx(Jmx jmx) throws Exception {
        newJson(jmx, false, "module", "console", "jmx");
    }

    @Override
    public void setJvm(Jvm jvm) throws Exception {
        for (Env env : jvm.getEnv()) {
            newJson(env, true, "jvm", "env");
        }
        for (Arg arg : jvm.getArg()) {
            newJson(arg, true, "jvm", "arg");
        }
    }

    @Override
    public void deleteJvm(String position) throws Exception {
        String jsonAll = Utils.read(jsonFile);
        String result = json.deleteJson(jsonAll,
                p -> true,
                "jvm", position);
        Utils.writeFile(jsonFile, result);
    }

    @Override
    public Jvm getJvm() {
        return readJsonFile(reader -> json.fromJson(reader, Jvm.class, "jvm"));
    }

    private void newJson(Object obj, boolean isInArray, String... position) throws Exception {
        Properties properties = this.json.fromJson(this.json.toJson(obj), Properties.class);

        String result;
        String json = Utils.read(jsonFile);
        if (isInArray) {
            result = this.json.addJson(json, properties, position);
        } else {
            result = this.json.setJson(json, properties, position);
        }

        Utils.writeFile(jsonFile, result);
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
