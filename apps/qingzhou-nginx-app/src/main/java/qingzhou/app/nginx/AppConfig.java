package qingzhou.app.nginx;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * 应用配置类
 */
public class AppConfig {

    private static final Properties APP_CONFIG = new Properties();
    private static final String NGINX_STATUS_URL_KEY = "nginx_status_url";
    public static final String NGINX_PATH_KEY = "nginx.path";
    public static final String NGINX_CONF_PATH_KEY = "nginx.path.conf";
    public static final String NGINX_CONF_BACKUPS_KEY = "nginx.path.backups";

    static {
        // 读取 config.properties 配置
        URL configUrl = AppConfig.class.getClassLoader().getResource("config.properties");
        if (configUrl != null) {
            try (InputStream in = configUrl.openStream()) {
                Properties defaultProps = new Properties();
                defaultProps.load(in);
                APP_CONFIG.putAll(defaultProps);
            } catch (Exception e) {
                System.err.println("加载配置异常:" + e.getMessage());
            }
        }
    }

    public static Properties getConfig() {
        return APP_CONFIG;
    }

    public static String getConfig(String key) {
        return APP_CONFIG.getProperty(key);
    }

    public static String getNginxStatusUrl() {
        return APP_CONFIG.getProperty(NGINX_STATUS_URL_KEY, "http://localhost/nginx_status");
    }

    public static String getNginxPath() {
        return APP_CONFIG.getProperty(NGINX_PATH_KEY, "");
    }

    public static String getNginxConfPath() {
        return APP_CONFIG.getProperty(NGINX_CONF_PATH_KEY, "");
    }

    public static String getNginxBackupPath() {
        return APP_CONFIG.getProperty(NGINX_CONF_BACKUPS_KEY, "");
    }

}
