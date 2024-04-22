package qingzhou.config;

import java.io.IOException;

public interface ConfigService {
    Config getConfig() throws IOException;
}
