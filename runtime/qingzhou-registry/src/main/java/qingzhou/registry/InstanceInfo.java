package qingzhou.registry;

public class InstanceInfo {
    public final String id;
    public final String name;
    public final String host;
    public final int port;
    public final String key;

    public InstanceInfo(String id, String name, String host, int port, String key) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.key = key;
    }
}
