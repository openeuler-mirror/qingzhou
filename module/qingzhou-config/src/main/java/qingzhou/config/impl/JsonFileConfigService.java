package qingzhou.config.impl;

import qingzhou.config.*;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;

public class JsonFileConfigService implements ConfigService {
    private final Json json;
    private final File jsonFile;

    public JsonFileConfigService(Json json, File jsonFile) {
        this.json = json;
        this.jsonFile = jsonFile;
    }

    @Override
    public Console getConsole() {
        return readJsonFile(reader -> json.fromJsonMember(reader, Console.class, "module", "console"));
    }

    @Override
    public Agent getAgent() {
        return readJsonFile(reader -> json.fromJsonMember(reader, Agent.class, "module", "agent"));
    }

    @Override
    public Heartbeat getHeartbeat() {
        return readJsonFile(reader -> json.fromJsonMember(reader, Heartbeat.class, "module", "heartbeat"));
    }

    @Override
    public void addUser(User user) {
        // todo
    }

    @Override
    public void deleteUser(User user) {
        // todo
    }

    @Override
    public void setUser(Jmx jmx) {
// todo
    }

    private <T> T readJsonFile(Callback<T> callback) {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile.toPath())) {
            return callback.useReader(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface Callback<T> {
        T useReader(Reader reader) throws Exception;
    }
}
