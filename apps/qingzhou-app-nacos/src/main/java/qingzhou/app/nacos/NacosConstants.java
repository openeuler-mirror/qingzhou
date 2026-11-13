package qingzhou.app.nacos;

/** Nacos 应用常量：默认值、Open API 路径与失败提示。 */
public class NacosConstants {
    // 默认值
    public static final String PUBLIC_NAMESPACE = "public";
    public static final String EMPTY_NAMESPACE = "";
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    public static final String DEFAULT_CLUSTER = "DEFAULT";
    public static final String DEFAULT_CONFIG_TYPE = "properties";
    public static final double DEFAULT_WEIGHT = 1.0;

    // API 路径
    public static final String AUTH_LOGIN_PATH = "/nacos/v1/auth/login";
    public static final String CONFIGS_PATH = "/nacos/v1/cs/configs";
    public static final String SERVICE_LIST_PATH = "/nacos/v1/ns/service/list";
    public static final String INSTANCE_LIST_PATH = "/nacos/v1/ns/instance/list";
    public static final String INSTANCE_PATH = "/nacos/v1/ns/instance";
    public static final String SERVICE_PATH = "/nacos/v1/ns/service";
    public static final String NAMESPACES_PATH = "/nacos/v1/console/namespaces";

    // 失败提示
    public static final String ERROR_PUBLIC_NAMESPACE_READONLY = "The public namespace cannot be modified or deleted.";
    public static final String ERROR_NAMESPACE_NOT_EXISTS = "Namespace does not exist";
    public static final String ERROR_CONFIG_PUBLISH_FAILED = "Publishing configuration failed";
    public static final String ERROR_CONFIG_UPDATE_FAILED = "Failed to update configuration";
    public static final String ERROR_CONFIG_DELETE_FAILED = "Failed to delete configuration";
    public static final String ERROR_SERVICE_REGISTER_FAILED = "Registration service failed";
    public static final String ERROR_SERVICE_DELETE_FAILED = "Failed to delete service";
    public static final String ERROR_NAMESPACE_CREATE_FAILED = "Namespace creation failed";
    public static final String ERROR_NAMESPACE_UPDATE_FAILED = "Namespace update failed";
    public static final String ERROR_NAMESPACE_DELETE_FAILED = "Namespace deletion failed";
}
