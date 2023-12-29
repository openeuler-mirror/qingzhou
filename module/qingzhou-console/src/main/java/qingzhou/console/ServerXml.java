package qingzhou.console;

import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ShowModel;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.Constants;
import qingzhou.console.util.ExceptionUtil;
import qingzhou.console.util.FileUtil;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;
import qingzhou.framework.impl.ServerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerXml { // todo 考虑由 admin service 来取代
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

    public static String getTenantUserNodeExpression(String tenant, String user) {
        if (StringUtil.isBlank(user)) {
            return "//root/console/auth/tenants/tenant[@" + ListModel.FIELD_NAME_ID + "='" + tenant + "']/users";
        }

        return "//root/console/auth/tenants/tenant[@" + ListModel.FIELD_NAME_ID + "='" + tenant + "']/users/user[@" + ListModel.FIELD_NAME_ID + "='" + user + "']";
    }

    public static String getTenantRoleNodeExpression(String tenant, String roleId) {
        if (StringUtil.isBlank(roleId)) {
            return "//root/console/auth/tenants/tenant[@" + ListModel.FIELD_NAME_ID + "='" + tenant + "']/roles";
        }

        return "//root/console/auth/tenants/tenant[@" + ListModel.FIELD_NAME_ID + "='" + tenant + "']/roles/role[@" + ListModel.FIELD_NAME_ID + "='" + roleId + "']";
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

    /********************** console ***********************/
    public static class ConsoleRole {
        // 开放的model，不需要检测权限
        // NOTE: 为方便自动测试集使用，此处设置为 public
        public static final String[] commonAppModels = {Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_index,
                Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_password,
                Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_favorites};
        public static final String[] openedModelActions;

        static {
            List<String> temp = new ArrayList<>();
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_index + "/" + Constants.ACTION_NAME_INDEX + "/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_index + "/home/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_home + "/" + ShowModel.ACTION_NAME_SHOW + "/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_password + "/key/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_password + "/validate/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_password + "/" + EditModel.ACTION_NAME_EDIT + "/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_password + "/" + EditModel.ACTION_NAME_UPDATE + "/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_favorites + "/" + ListModel.ACTION_NAME_LIST + "/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_favorites + "/addfavorite/");
            temp.add("/" + Constants.QINGZHOU_MASTER_APP_NAME + "/" + Constants.MODEL_NAME_favorites + "/cancelfavorites/");
            openedModelActions = temp.toArray(new String[0]);
        }

        public static boolean isRootUser(String loginUser) {
            String tenant = getTenant(loginUser);
            String loginUserName = getLoginUserName(loginUser);
            Map<String, String> user = new XmlUtil(ServerUtil.getServerXml()).getAttributes(getTenantUserNodeExpression(tenant, loginUserName));
            if (user != null && !user.isEmpty()) {
                String role = user.get("roles");
                if (StringUtil.notBlank(role)) {
                    String[] roles = role.split(Constants.DATA_SEPARATOR);
                    for (String r : roles) {
                        if (BuiltinRoleEnum.root.name().equals(r)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public static boolean isAuditorModel(String model) {
            for (String m : BuiltinRoleEnum.auditor.getModels()) {
                if (m.equals(model)) {
                    return true;
                }
            }

            return false;
        }

        public static boolean checkLoginUserIsManagerRole(String loginUser, boolean containTenant) {
            String tenant = getTenant(loginUser);
            String loginUserName = getLoginUserName(loginUser);
            Map<String, String> user = new XmlUtil(ServerUtil.getServerXml()).getAttributes(getTenantUserNodeExpression(tenant, loginUserName));
            if (user != null && !user.isEmpty()) {
                String role = user.get("roles");
                if (StringUtil.notBlank(role)) {
                    String[] roles = role.split(Constants.DATA_SEPARATOR);
                    for (String r : roles) {
                        if (BuiltinRoleEnum.root.name().equals(r)
                                || BuiltinRoleEnum.system.name().equals(r)
                                || (containTenant && BuiltinRoleEnum.tenant.name().equals(r))) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public static List<String> systemXUsers() {
            return new ArrayList<String>() {{
                add("thanos");
                add("security");
                add("auditor");
                add("monitor");
            }};
        }

        public static List<String> systemXRoles() {
            List<String> roles = new ArrayList<>();
            for (BuiltinRoleEnum role : BuiltinRoleEnum.values()) {
                roles.add(role.name());
            }

            return roles;
        }

        public enum BuiltinRoleEnum {
            root("超级管理员", null, null, null, null),
            system("系统管理员", null, null
                    , null, new String[]{
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_instance,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_cluster,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_appversion,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_backup,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_user,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_role,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_userrole,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_auditlog,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_auditconfig
            }),
            tenant("租户管理员", new String[]{Constants.QINGZHOU_MASTER_APP_NAME}, new String[]{
                    Constants.MODEL_NAME_instance,
                    Constants.MODEL_NAME_cluster,
                    Constants.MODEL_NAME_appversion,
                    Constants.MODEL_NAME_backup,
                    Constants.MODEL_NAME_user,
                    Constants.MODEL_NAME_role,
                    Constants.MODEL_NAME_userrole
            }, new String[]{
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_node + Constants.GROUP_SEPARATOR + ListModel.ACTION_NAME_SHOW,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_node + Constants.GROUP_SEPARATOR + ListModel.ACTION_NAME_LIST,
                    Constants.QINGZHOU_MASTER_APP_NAME + Constants.GROUP_SEPARATOR + Constants.MODEL_NAME_node + Constants.GROUP_SEPARATOR + EditModel.ACTION_NAME_EDIT
            }, null),
            auditor("安全审计员", new String[]{Constants.QINGZHOU_MASTER_APP_NAME},
                    new String[]{
                            Constants.MODEL_NAME_index,
                            Constants.MODEL_NAME_password,
                            Constants.MODEL_NAME_favorites,
                            Constants.MODEL_NAME_auditlog,
                            Constants.MODEL_NAME_auditconfig
                    }
                    , null, null);

            private String info;
            private String[] apps;
            private String[] models;
            private String[] extendedUris;
            private String[] excludedUris;

            BuiltinRoleEnum(String info, String[] apps, String[] models, String[] extendedUris, String[] excludedUris) {
                this.info = info;
                this.apps = apps;
                this.models = models;
                this.extendedUris = extendedUris;
                this.excludedUris = excludedUris;
            }

            public String getInfo() {
                return info;
            }

            public String[] getApps() {
                return apps;
            }

            public String[] getModels() {
                return models;
            }

            /**
             * 扩展增加master应用的功能菜单权限
             * 1. app/model         增加mapp/odel所有的action权限
             * 2. app/model/action  增加app/model指定action的权限
             *
             * @return
             */
            public String[] getExtendedUris() {
                return extendedUris;
            }

            /**
             * 排除的master应用功能菜单权限
             * 1. app/model         增加app的model所有的action权限
             * 2. app/model/action  增加app的model指定action的权限
             *
             * @return
             */
            public String[] getExcludedUris() {
                return excludedUris;
            }
        }
    }
}
