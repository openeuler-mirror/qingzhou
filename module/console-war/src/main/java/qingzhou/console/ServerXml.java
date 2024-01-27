package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.framework.util.XmlUtil;

import java.io.File;
import java.util.Map;

public class ServerXml { // todo 考虑替代：ConfigManager
    private static File serverXmlFile;

    public static ServerXml get() {
        return ServerXml.get(getServerXml());
    }

    public static File getServerXml() {
        if (serverXmlFile == null) {
            try {
                serverXmlFile = FileUtil.newFile(ConsoleWarHelper.getDomain(), "conf", "server.xml");
            } catch (Exception ignored) {
            }
        }
        return serverXmlFile;
    }

    public static ServerXml get(File xmlFile) {
        ServerXml xml = new ServerXml();
        xml.xmlFile = xmlFile;
        return xml;
    }

    private File xmlFile;

    private ServerXml() {
    }

    public Map<String, String> server() {
        return getAttributes("server");
    }

    public Map<String, String> security() {
        return getAttributes("server/security");
    }

    private Map<String, String> getAttributes(String path) {
        XmlUtil xmlUtil = new XmlUtil(this.xmlFile);
        return xmlUtil.getAttributes("/root/" + path);
    }

    /********************** console ***********************/
    public Map<String, String> getNodeById(String id) {
        return new XmlUtil(this.xmlFile).getAttributesByKey("/root/console/nodes/node", "id", id);
    }

    public Map<String, String> getClusterById(String id) {
        return new XmlUtil(this.xmlFile).getAttributesByKey("/root/console/clusters/cluster", "id", id);
    }

    public boolean isDisableUpload() {
        return isEnabled("/root/console", "disableUpload");
    }

    public boolean isDisableDownload() {
        return isEnabled("/root/console", "disableDownload");
    }

    public boolean verCodeEnabled() {
        return isEnabled("/root/console/auth", "verCodeEnabled");
    }

    private boolean isEnabled(String nodeExpression, String specifiedKey) {
        return new XmlUtil(this.xmlFile).getBooleanAttribute(nodeExpression, specifiedKey, true);
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
        return new XmlUtil(this.xmlFile).getSpecifiedAttribute("/root/console", specifiedKey);
    }

    public Map<String, String> user(String loginUser) {
        String tenant = getTenant(loginUser);
        String userName = getLoginUserName(loginUser);
        Map<String, String> attributes = getAttributes("console/auth/tenants/tenant[@id='" + tenant + "']/users/user[@id='" + userName + "']");

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
