package qingzhou.config.impl;

import qingzhou.config.Module;
import qingzhou.config.ConfigService;
import qingzhou.engine.util.Utils;
import qingzhou.json.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigServiceImpl implements ConfigService {
    private final Json json;
    private final File instanceDir;
    private Module cache;

    public ConfigServiceImpl(Json json, File instanceDir) {
        this.json = json;
        this.instanceDir = instanceDir;
    }

    @Override
    public Module getModule() throws IOException {
        if (cache == null) {
            try (InputStream inputStream = Files.newInputStream(new File(instanceDir, "qingzhou.json").toPath())) {
                String read = Utils.read(inputStream);
                cache = json.fromJsonMember(read, "module", Module.class);
            }
        }
        return cache;
    }
}
