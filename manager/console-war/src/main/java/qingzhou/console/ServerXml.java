package qingzhou.console;

import qingzhou.console.controller.SystemController;

import java.util.Map;

public class ServerXml { // todo 考虑替代：ConfigManager
    private static final ServerXml instance = new ServerXml();

    public static ServerXml get() {
        return instance;
    }

    public Map<String, String> server() throws Exception {
        return SystemController.getConfig().getDataById("server", null);
    }

    /********************** console ***********************/
    public Map<String, String> getNodeById(String id) throws Exception {
        return SystemController.getConfig().getDataById("node", id);
    }

    public boolean verCodeEnabled() {
        try {
            return Boolean.parseBoolean(SystemController.getConfig().getDataById("auth", null).get("verCodeEnabled"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isJmxEnabled() throws Exception {
        Map<String, String> config = SystemController.getConfig().getDataById("jmx", null);
        return config != null && Boolean.parseBoolean(config.getOrDefault("enabled", "false"));
    }

    public Map<String, String> jmx() {
        try {
            return SystemController.getConfig().getDataById("jmx", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String trustedIP() {
        return consoleAttribute("trustedIP");
    }

    public String lockOutTime() {
        return consoleAttribute("lockOutTime");
    }

    public String failureCount() {
        return consoleAttribute("failureCount");
    }

    private String consoleAttribute(String specifiedKey) {
        try {
            return SystemController.getConfig().getDataById("console", null).get(specifiedKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> user(String loginUser) throws Exception {
        String userName = getLoginUserName(loginUser);
        Map<String, String> attributes = SystemController.getConfig().getDataById("user", userName);

        if (!Boolean.parseBoolean(attributes.get("active"))) {
            return null;
        }

        return attributes;
    }

    public static String getLoginUserName(String loginUser) {
        if (StringUtil.isBlank(loginUser)) {
            return null;
        }
        int index = loginUser.indexOf("/");
        if (index == -1) {
            return loginUser;
        } else {
            return loginUser.substring(index + 1);
        }
    }
}
