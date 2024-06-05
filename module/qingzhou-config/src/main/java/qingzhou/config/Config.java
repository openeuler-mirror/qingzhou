package qingzhou.config;

public interface Config {
    Console getConsole();

    Agent getAgent();

    Heartbeat getHeartbeat();

    Jvm getJvm();

    void setJmx(Jmx jmx) throws Exception;

    void addUser(User user) throws Exception;

    void deleteUser(String id) throws Exception;

    void addEnv(Env env) throws Exception;

    void deleteEnv(String id) throws Exception;

    void addArg(Arg arg) throws Exception;

    void deleteArg(String id) throws Exception;

    Security getSecurity();

    void setSecurity(Security security) throws Exception;
}
