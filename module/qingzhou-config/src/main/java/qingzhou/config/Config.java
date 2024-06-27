package qingzhou.config;

public interface Config {
    Console getConsole();

    Agent getAgent();

    Heartbeat getHeartbeat();

    Jvm getJvm();

    Node[] getNode();

    void addUser(User user) throws Exception;

    void deleteUser(String id) throws Exception;

    void addEnv(Env env) throws Exception;

    void deleteEnv(String id) throws Exception;

    void addArg(Arg arg) throws Exception;

    void deleteArg(String id) throws Exception;

    void setJmx(Jmx jmx) throws Exception;

    void setSecurity(Security security) throws Exception;

    void addNode(Node node) throws Exception;

    void deleteNode(String id) throws Exception;
}
