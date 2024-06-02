package qingzhou.config;

import java.io.File;

public interface Config {
    File getConfigFile();

    Console getConsole();

    Agent getAgent();

    Heartbeat getHeartbeat();

    void addUser(User user) throws Exception;

    void deleteUser(User user) throws Exception;

    void setJmx(Jmx jmx) throws Exception;
}
