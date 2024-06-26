package qingzhou.config.impl;

import qingzhou.config.*;
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
        writeJson(user, true, "module", "console", "user");
    }

    @Override
    public void deleteUser(String id) throws IOException {
        deleteJson("id", id, "module", "console", "user");
    }

    @Override
    public void addEnv(Env env) throws Exception {
        writeJson(env, true, "jvm", "env");
    }

    @Override
    public void deleteEnv(String id) throws Exception {
        deleteJson("name", id, "jvm", "env");
    }

    @Override
    public void addArg(Arg arg) throws Exception {
        writeJson(arg, true, "jvm", "arg");
    }

    @Override
    public void deleteArg(String id) throws Exception {
        deleteJson("name", id, "jvm", "arg");
    }

    @Override
    public void setJmx(Jmx jmx) throws Exception {
        writeJson(jmx, false, "module", "console", "jmx");
    }

    @Override
    public Jvm getJvm() {
        return readJsonFile(reader -> json.fromJson(reader, Jvm.class, "jvm"));
    }

    @Override
    public Node[] getNode() {
        return readJsonFile(reader -> json.fromJson(reader, Node[].class, "module", "node"));
    }

    @Override
    public void setSecurity(Security security) throws Exception {
        writeJson(security, false, "module", "console", "security");
    }

    @Override
    public void addNode(Node node) throws Exception {
        writeJson(node, true, "module", "node");
    }

    @Override
    public void deleteNode(String id) throws Exception {
        deleteJson("id", id, "module", "node");
    }

    public void deleteJson(String idKey, String idVal, String... position) throws IOException {
        String jsonAll = Utils.read(jsonFile);
        String result = json.deleteJson(jsonAll,
                p -> p.getProperty(idKey).equals(idVal),
                position);
        writeFile(result);
    }

    private void writeJson(Object obj, boolean isInArray, String... position) throws Exception {
        Properties properties = this.json.fromJson(this.json.toJson(obj), Properties.class);

        String result;
        String json = Utils.read(jsonFile);
        if (isInArray) {
            result = this.json.addJson(json, properties, position);
        } else {
            result = this.json.setJson(json, properties, position);
        }

        writeFile(result);
    }

    private void writeFile(String result) throws IOException {
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
