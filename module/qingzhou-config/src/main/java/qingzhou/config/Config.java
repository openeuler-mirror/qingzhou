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

    void setJvm(Jvm jvm) throws Exception;

    void deleteJvm(String position) throws Exception;

    Jvm getJvm();
}
