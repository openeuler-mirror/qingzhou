package qingzhou.config.impl;

import qingzhou.config.Config;
import qingzhou.config.ConfigService;
import qingzhou.engine.util.FileUtil;
import qingzhou.json.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigServiceImpl implements ConfigService {
    private final Json json;
    private final File instanceDir;
    private Config cache;

    public ConfigServiceImpl(Json json, File instanceDir) {
        this.json = json;
        this.instanceDir = instanceDir;
    }

    @Override
    public Config getConfig() throws IOException {
        if (cache == null) {
            try (InputStream inputStream = Files.newInputStream(new File(instanceDir, "qingzhou.json").toPath())) {
                String read = FileUtil.read(inputStream);
                cache = json.fromJson(read, Config.class);
            }
        }
        return cache;
    }
}
