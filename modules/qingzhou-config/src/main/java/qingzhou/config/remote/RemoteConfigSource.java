package qingzhou.config.remote;

import java.util.Map;

public interface RemoteConfigSource {
    Map<String, String> pull() throws Exception;
}
