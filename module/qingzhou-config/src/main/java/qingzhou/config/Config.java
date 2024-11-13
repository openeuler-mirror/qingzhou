package qingzhou.config;

import qingzhou.engine.Service;

@Service(shareable = false)
public interface Config {
    Console getConsole();

    void addUser(User user) throws Exception;

    void deleteUser(String... id) throws Exception;

    void setWeb(Web web) throws Exception;

    void setJmx(Jmx jmx) throws Exception;

    void setSecurity(Security security) throws Exception;
}
