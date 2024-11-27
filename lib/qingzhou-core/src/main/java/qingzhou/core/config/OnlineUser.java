package qingzhou.core.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OnlineUser {
    private final Map<String, Long> onlineUsers = Collections.synchronizedMap(new HashMap<>());

    public void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public void addUser(String username, Long loginTime) {
        onlineUsers.put(username, loginTime);
    }

    public Map<String, Long> getOnlineUser() {
        return onlineUsers;
    }
}
