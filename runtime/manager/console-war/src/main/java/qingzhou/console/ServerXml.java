package qingzhou.console;

import qingzhou.console.controller.SystemController;
import qingzhou.framework.util.StringUtil;

import java.util.Map;

public class ServerXml { // todo 考虑替代：ConfigManager
    private static final ServerXml instance = new ServerXml();

    public static ServerXml get() {
        return instance;
    }

    public Map<String, String> server() {
        return getAttributes("server");
    }

    private Map<String, String> getAttributes(String path) {
        return SystemController.getConfig().getConfig("/root/" + path);
    }

    /********************** console ***********************/
    public Map<String, String> getNodeById(String id) {
        return SystemController.getConfig().getConfig(String.format("/root/console/nodes/node[@%s='%s']", "id", id));
    }

    public boolean verCodeEnabled() {
        return Boolean.parseBoolean(SystemController.getConfig().getConfig("/root/console/auth").get("verCodeEnabled"));
    }

    public boolean isJmxEnabled() {
        Map<String, String> config = SystemController.getConfig().getConfig("//jmx");
        return config != null && Boolean.parseBoolean(config.getOrDefault("enabled", "false"));
    }

    public Map<String, String> jmx() {
        return SystemController.getConfig().getConfig("//jmx");
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
        return SystemController.getConfig().getConfig("/root/console").get(specifiedKey);
    }

    public Map<String, String> user(String loginUser) {
        String userName = getLoginUserName(loginUser);
        Map<String, String> attributes = getAttributes("console/auth/users/user[@id='" + userName + "']");

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
