package com.tongtech.zookeeper.starter.service;

import com.tongtech.zookeeper.starter.ZookeeperApp;
import com.tongtech.zookeeper.starter.util.Util;
import qingzhou.api.ModelBase;
import qingzhou.http.client.HttpMethod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 调用 Zookeeper admin server 命令接口的公共基类，
 * 统一处理元数据读取、版本校验、HTTP 请求与结果格式化。
 */
public abstract class AbstractZkCommandService extends ModelBase {

    /**
     * admin 命令接口的子路径，如 configuration/server_stats/connections。
     * 不需要访问 admin server 的模型无需覆写。
     */
    protected String getSubPath() {
        return null;
    }

    protected String findMetaDataProperties(String key) {
        Properties properties = getAppContext().getProperties();
        if (properties == null) {
            return null;
        }
        Properties metadata = (Properties) properties.get(ZookeeperApp.METADATA_KEY);
        if (metadata == null) {
            return null;
        }
        return metadata.getProperty(key);
    }

    /** 校验 Zookeeper 版本不低于 3.5.0 且已开启 admin server */
    protected void checkJetty() {
        String version = findMetaDataProperties(ZookeeperApp.METADATA_VERSION_KEY);
        if (!Util.comparativeVersion(version)) {
            throw new RuntimeException("This function cannot be used if the version is earlier than 3.5.0");
        }
        String url = findMetaDataProperties(ZookeeperApp.METADATA_COMMANDS_KEY);
        if (Util.isBlank(url)) {
            throw new RuntimeException("admin.enableServer is not enabled");
        }
    }

    protected String buildUrl() {
        return findMetaDataProperties(ZookeeperApp.METADATA_COMMANDS_KEY) + "/" + getSubPath();
    }

    /** 向 Zookeeper admin server 发起 GET 请求并解析 JSON 响应 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> sendHttp() throws Exception {
        String url = buildUrl();
        if (ZookeeperApp.logger.isDebugEnabled()) {
            ZookeeperApp.logger.debug("send url " + url);
        }
        String data = ZookeeperApp.sendHttp(ZookeeperApp.httpClient.newRequest(url).method(HttpMethod.GET));
        if (Util.notBlank(data)) {
            return ZookeeperApp.json.fromJson(data, HashMap.class);
        }
        return new HashMap<>();
    }

    /**
     * 将响应中的值统一转换为字符串并过滤 null 值，
     * 浮点数去除多余的小数尾零。
     */
    protected static Map<String, String> convertValuesToString(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Double) {
                result.put(entry.getKey(), new BigDecimal((Double) value).stripTrailingZeros().toPlainString());
            } else {
                result.put(entry.getKey(), String.valueOf(value));
            }
        }
        return result;
    }
}
