package qingzhou.config;

public interface Config {
    Console getConsole();

    Agent getAgent();

    Jvm getJvm();

    void addUser(User user) throws Exception;

    void deleteUser(String id) throws Exception;

    void addDepartment(Department department) throws Exception;

    void deleteDepartment(String id) throws Exception;

    void addEnv(Env env) throws Exception;

    void deleteEnv(String id) throws Exception;

    void addArg(Arg arg) throws Exception;

    void deleteArg(String id) throws Exception;

    void setJmx(Jmx jmx) throws Exception;

    void setSecurity(Security security) throws Exception;
}
