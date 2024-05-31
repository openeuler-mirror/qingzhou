package qingzhou.config;

public interface ConfigService {
    Console getConsole();

    Agent getAgent();

    Heartbeat getHeartbeat();

    void addUser(User user);

    void deleteUser(User user);

    void setUser(Jmx jmx);
}
