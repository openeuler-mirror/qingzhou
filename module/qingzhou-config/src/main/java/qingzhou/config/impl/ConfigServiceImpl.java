package qingzhou.config.impl;

import qingzhou.config.Config;
import qingzhou.config.ConfigService;
import qingzhou.json.Json;

public class ConfigServiceImpl implements ConfigService {
    private final Json json;

    public ConfigServiceImpl(Json json) {
        this.json = json;
    }

    @Override
    public Config getConfig() {
        return null;
    }
}
