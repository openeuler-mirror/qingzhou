package qingzhou.config;

import java.io.IOException;

public interface ConfigService {
    Module getModule() throws IOException;
}
