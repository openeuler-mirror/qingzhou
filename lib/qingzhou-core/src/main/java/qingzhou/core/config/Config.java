package qingzhou.core.config;

import qingzhou.engine.Service;

@Service(shareable = false)
public interface Config {
    Core getCore();

    void addUser(User user) throws Exception;

    void deleteUser(String... id) throws Exception;

    void setWeb(Web web) throws Exception;

    void setJmx(Jmx jmx) throws Exception;

    void setSecurity(Security security) throws Exception;

    OnlineUser getOnlineUser();

    void addRole(Role role) throws Exception;

    void deleteRole(String... id) throws Exception;

}
