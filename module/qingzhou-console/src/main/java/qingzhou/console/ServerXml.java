package qingzhou.console;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.*;

import java.io.File;
import java.util.*;

public class ServerXml {
    private static File serverXmlFile;

    public static ServerXml get() {
        return ServerXml.get(getServerXml());
    }

    public static File getServerXml() {
        if (serverXmlFile == null) {
            try {
                serverXmlFile = FileUtil.newFile(ConsoleWarHelper.getDomain(), "conf", "qingzhou.xml");
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

    public Map<String, String> serverLog() {
        return getAttributes("server/loggers/server");
    }

    public Map<String, String> security() {
        return getAttributes("server/security");
    }

    public List<Map<String, String>> startArgs() {
        return getAttributesList("java-config/start-args/arg");
    }

    public List<Map<String, String>> args() {
        return getAttributesList("java-config/arg");
    }

    public List<Map<String, String>> envList() {
        return getAttributesList("java-config/environments/env");
    }

    public Map<String, String> javaConfig() {
        return getAttributes("java-config");
    }

    public Map<String, String> connector() {
        return getAttributes("server/connector");
    }

    public Map<String, String> getNodeAttributes(String nodeExpression) {
        return new XmlUtil(this.xmlFile).getAttributes(nodeExpression);
    }

    private Map<String, String> getAttributes(String path) {
        XmlUtil xmlUtil = new XmlUtil(this.xmlFile);
        return xmlUtil.getAttributes("/root/" + path);
    }

    private List<Map<String, String>> getAttributesList(String path) {
        XmlUtil xmlUtil = new XmlUtil(this.xmlFile);
        try {
            return xmlUtil.getAttributesList("/root/" + path);
        } catch (Exception e) {
            throw ExceptionUtil.unexpectedException(e);
        }
    }

    /********************** console ***********************/
    public Map<String, String> getNodeById(String id) {
        return new XmlUtil(this.xmlFile).getAttributesByKey("/root/console/nodes/node", "id", id);
    }

    public Map<String, String> getInstanceById(String id) {
        return new XmlUtil(this.xmlFile).getAttributesByKey("/root/console/instances/instance", "id", id);
    }

    public List<String> getAllInstanceIdByCluster(String clusterName) {
        return new XmlUtil(this.xmlFile).getSpecifiedListAttributeByAttr("instance", "id", Constants.MODEL_NAME_cluster, clusterName);
    }

    public String getConsoleDisabled() {
        return new XmlUtil(this.xmlFile).getSpecifiedAttribute("/root/console", "disabled");
    }

    public boolean isDisableUpload() {
        return isEnabled("/root/console", "disableUpload", true);
    }

    public boolean isDisableDownload() {
        return isEnabled("/root/console", "disableDownload", true);
    }

    public boolean verCodeEnabled() {
        return isEnabled("/root/console/auth", "verCodeEnabled", true);
    }

    public boolean authEnabled() {
        return isEnabled("/root/console/auth", "enabled", true);
    }

    private boolean isEnabled(String nodeExpression, String specifiedKey, boolean defaultVal) {
        return new XmlUtil(this.xmlFile).getBooleanAttribute(nodeExpression, specifiedKey, defaultVal);
    }

    public String trustedIP() {
        return consoleAttribute("trustedIP");
    }

    public String extendedCharset() {
        return consoleAttribute("extendedCharset");
    }

    public String lockOutTime() {
        return consoleAttribute("lockOutTime");
    }

    public String failureCount() {
        return consoleAttribute("failureCount");
    }

    private String consoleAttribute(String specifiedKey) {
        return new XmlUtil(this.xmlFile).getSpecifiedAttribute("/root/console", specifiedKey);
    }

    public boolean jmxEnabled() {
        return Boolean.parseBoolean(jmx().get("enabled"));
    }

    public Map<String, String> jmx() {
        return getAttributes("console/jmx");
    }

    public Map<String, String> auditing() {
        return getAttributes("console/auditing");
    }

    public List<Map<String, String>> userList(String tenant) {
        return getAttributesList("console/auth/tenants/tenant[@id='" + tenant + "']/users/user");
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
        int index = loginUser.indexOf(Constants.GROUP_SEPARATOR);
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
        int index = loginUser.indexOf(Constants.GROUP_SEPARATOR);
        if (index == -1) {
            return "default";
        } else {
            return loginUser.substring(0, index);
        }
    }

    public static String getAppName(String targetType, String targetName) {
        if (Constants.MODEL_NAME_instance.equals(targetType)) {
            if (Constants.QINGZHOU_MASTER_APP_NAME.equals(targetName)
                    || Constants.QINGZHOU_DEFAULT_APP_NAME.equals(targetName)
                    || "domain1".equals(targetName)) {
                return targetName;
            }
            Map<String, String> instanceById = get().getInstanceById(targetName);
            if (instanceById != null) {
                return instanceById.get("app");
            }
        } else if (Constants.MODEL_NAME_cluster.equals(targetType)) {
            List<String> instances = get().getAllInstanceIdByCluster(targetName);
            if (instances != null) {
                Map<String, String> instanceById = get().getInstanceById(instances.get(0));
                if (instanceById != null) {
                    return instanceById.get("app");
                }
            }
        }
        return null;
    }

    public static List<String> getMyFavorites(String loginUser) {
        List<String> result = new ArrayList<>(); // 返回 List 而不是 Set 可控制收藏菜单的顺序
        if (StringUtil.isBlank(loginUser)) {
            return Collections.emptyList();
        }
        try {
            Map<String, String> user = get().user(loginUser);
            String favorites = user.get("favorites");
            if (!StringUtil.isBlank(favorites)) {
                String[] userFavorites = favorites.split(",");
                result.addAll(new ArrayList<>(Arrays.asList(userFavorites)));
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    public static List<String> getInstanceFavorites(String loginUser, String instanceName) {
        List<String> myFavorites = getMyFavorites(loginUser);
        List<String> favorites = new ArrayList<>();
        for (String myFavorite : myFavorites) {
            if (myFavorite.startsWith(instanceName)) {
                favorites.add(myFavorite);
            }
        }

        return favorites;
    }

    public static boolean isMyFavorites(String loginUser, String instanceName, String model, String action) {
        return getMyFavorites(loginUser).contains(instanceName + "/" + model + "/" + action);
    }
}
