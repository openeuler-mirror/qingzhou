package qingzhou.config.impl;

import qingzhou.config.Config;
import qingzhou.config.ConfigService;
import qingzhou.engine.ServiceRegister;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Controller extends ServiceRegister<ConfigService> {
    private ConfigService configService;

    @Override
    public Class<ConfigService> serviceType() {
        return ConfigService.class;
    }

    @Override
    protected ConfigService serviceObject() {
        return configService;
    }

    @Override
    protected void startService() throws Exception {
        configService = new ConfigService() {
            private final String jsonContent;

            {
                File instanceDir = moduleContext.getInstanceDir();
                StringBuilder fileContent = new StringBuilder();
                try (InputStream inputStream = Files.newInputStream(new File(instanceDir, "qingzhou.json").toPath())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    for (String line; (line = reader.readLine()) != null; ) {
                        fileContent.append(line);
                    }
                }
                jsonContent = fileContent.toString().replace("${qingzhou.instance}", instanceDir.getAbsolutePath());
            }

            @Override
            public Config getConfig() {
                Json json = moduleContext.getService(Json.class);
                // 不要缓存这个结果，它可能被其它模块修改状态，此处的办法是每次重新转换，其实也可以克隆备份
                return json.fromJson(jsonContent, Config.class);
            }
        };
    }
}
