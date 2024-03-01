package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
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

    public Map<String, String> security() {
        return getAttributes("server/security");
    }

    private Map<String, String> getAttributes(String path) {
        return ConsoleWarHelper.getConfig().getConfig("/root/" + path);
    }

    /********************** console ***********************/
    public Map<String, String> getNodeById(String id) {
        return ConsoleWarHelper.getConfig().getConfig(String.format("/root/console/nodes/node[@%s='%s']", "id", id));
    }

    public boolean verCodeEnabled() {
        return Boolean.parseBoolean(ConsoleWarHelper.getConfig().getConfig("/root/console/auth").get("verCodeEnabled"));
    }

    public boolean isJmxEnabled() {
        Map<String, String> config = ConsoleWarHelper.getConfig().getConfig("//jmx");
        return config != null && Boolean.parseBoolean(config.getOrDefault("enabled", "false"));
    }

    public Map<String, String> jmx() {
        return ConsoleWarHelper.getConfig().getConfig("//jmx");
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

    public String contextRoot() {
        return consoleAttribute("contextRoot");
    }

    private String consoleAttribute(String specifiedKey) {
        return ConsoleWarHelper.getConfig().getConfig("/root/console").get(specifiedKey);
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

    public static String getTenant(String loginUser) {
        if (StringUtil.isBlank(loginUser)) {
            return null;
        }
        int index = loginUser.indexOf("/");
        if (index == -1) {
            return "default";
        } else {
            return loginUser.substring(0, index);
        }
    }
}
