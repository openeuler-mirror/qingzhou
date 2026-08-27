package qingzhou.app.nacos;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.type.*;

@Model(code = "nacos-config", order = 3,
        name = {"配置列表", "en:Config Management"},
        info = {"Nacos配置管理中心", "en:Nacos Configuration Management"},
        icon = "Setting")
public class NacosConfig extends NacosModelBase implements Page, Show, Add, Update, Delete {

    private Map<String, Map<String, String>> configCache = new HashMap<>();

    @ModelField(id = true,
            name = {"ID", "en:ID"},
            list = true,
            show = true,
            add = false,
            required = true)
    public String id;

    @ModelField(
            name = {"命名空间ID", "en:Namespace"},
            list = true,
            show = false,
            add = true,
            update = false,
            search = true,
            required = true
    )
    public String namespace;

    @ModelField(
            name = {"Data Id", "en:Data ID"},
            list = true,
            add = true,
            update = false,
            required = true
    )
    public String dataId;

    @ModelField(
            name = {"GROUP", "en:Group"},
            list = true,
            show = true,
            add = true,
            update = true,
            required = true
    )
    public String group = "DEFAULT_GROUP";

    @ModelField(
            name = {"归属应用", "en:Belong App"},
            list = true,
            show = false,
            add = false,
            update = false)
    public String belongApp;

    @ModelField(
            name = {"MD5", "en:MD5"},
            add = false,
            update = false,
            show = true
    )
    public String md5;

    @ModelField(
            name = {"配置格式", "en:Type"},
            list = true,
            show = false,
            add = true,
            update = true,
            input_type = InputType.select,
            options = {"text", "json", "xml", "yaml", "html", "properties"}
    )
    public String type;

    @ModelField(
            name = {"内容", "en:Content"},
            show = true,
            add = true,
            update = true,
            input_type = InputType.textarea,
            required = true
    )
    public String content;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        List<String[]> result = new ArrayList<>();
        
        try {
            NacosApi nacosApi = getNacosApi();
            String namespaceId = query != null ? query.get("namespace") : null;
            String tenant = getEffectiveNamespaceId(namespaceId);
            
            configCache.clear();
            
            Map<String, Object> data = nacosApi.getConfigs(pageNum, pageSize, tenant);
            List<?> configs = (List<?>) data.get("pageItems");
            
            if (configs != null) {
                for (Object configObj : configs) {
                    Map<String, Object> config = (Map<String, Object>) configObj;
                    String configId = String.valueOf(config.get("id"));
                    String configDataId = String.valueOf(config.get("dataId"));
                    String configGroup = String.valueOf(config.get("group"));
                    String configType = String.valueOf(config.get("type"));
                    
                    Map<String, String> configInfo = new HashMap<>();
                    configInfo.put("id", configId);
                    configInfo.put("namespace", isPublicNamespace(namespaceId) ? NacosConstants.PUBLIC_NAMESPACE : namespaceId);
                    configInfo.put("dataId", configDataId);
                    configInfo.put("group", configGroup);
                    configInfo.put("tenant", tenant);
                    configInfo.put("type", configType);
                    configInfo.put("belongApp", String.valueOf(config.get("appName")));
                    
                    configCache.put(configId, configInfo);
                    
                    String[] row = new String[listFields.length];
                    for (int j = 0; j < listFields.length; j++) {
                        row[j] = configInfo.getOrDefault(listFields[j], "");
                    }
                    result.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        try {
            NacosApi nacosApi = getNacosApi();
            String namespaceId = query != null ? query.get("namespace") : null;
            String tenant = getEffectiveNamespaceId(namespaceId);
            
            Map<String, Object> data = nacosApi.getConfigs(1, 1, tenant);
            return ((Number) data.get("totalCount")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        return true;
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> result = new HashMap<>();
        
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        
        Map<String, String> cachedConfig = configCache.get(id);
        String dataId = "";
        String group = "";
        String tenant = "";
        
        if (cachedConfig != null) {
            dataId = cachedConfig.get("dataId");
            group = cachedConfig.get("group");
            tenant = cachedConfig.get("tenant");
            result.putAll(cachedConfig);
        }

        try {
            NacosApi nacosApi = getNacosApi();
            
            result.put("content", nacosApi.getConfigContent(dataId, group, tenant));
            
            Map<String, Object> configInfo = nacosApi.getConfigInfo(dataId, group, tenant);
            if (result.get("type") == null) {
                result.put("type", String.valueOf(configInfo.get("type")));
            }
            if (result.get("tenant") == null) {
                result.put("tenant", String.valueOf(configInfo.get("tenant")));
            }
            result.put("md5", String.valueOf(configInfo.get("md5")));
            result.put("belongApp", String.valueOf(configInfo.get("appName")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        // 输入验证
        String dataId = data.get("dataId");
        String group = data.get("group");
        String content = data.get("content");
        
        group = getOrDefault(group, NacosConstants.DEFAULT_GROUP);
        String type = getOrDefault(data.get("type"), NacosConstants.DEFAULT_CONFIG_TYPE);
        String namespaceId = data.get("namespace");
        String tenant = getEffectiveNamespaceId(namespaceId);
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.publishConfig(dataId, group, content, type, tenant);
            if (!success) {
                handleFailure(NacosConstants.ERROR_CONFIG_PUBLISH_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_CONFIG_PUBLISH_FAILED, e);
        }
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        
        String dataId = "";
        String group = "";
        String tenant = "";
        
        Map<String, String> cachedConfig = configCache.get(id);
        if (cachedConfig != null) {
            dataId = cachedConfig.get("dataId");
            group = cachedConfig.get("group");
            tenant = cachedConfig.get("tenant");
        }
        
        String content = data.get("content");
        String type = getOrDefault(data.get("type"), NacosConstants.DEFAULT_CONFIG_TYPE);
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.publishConfig(dataId, group, content, type, tenant);
            if (!success) {
                handleFailure(NacosConstants.ERROR_CONFIG_UPDATE_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_CONFIG_UPDATE_FAILED, e);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String dataId = "";
        String group = "";
        String tenant = "";
        
        Map<String, String> cachedConfig = configCache.get(id);
        if (cachedConfig != null) {
            dataId = cachedConfig.get("dataId");
            group = cachedConfig.get("group");
            tenant = cachedConfig.get("tenant");
        }
        
        try {
            NacosApi nacosApi = getNacosApi();
            boolean success = nacosApi.deleteConfig(dataId, group, tenant);
            if (!success) {
                handleFailure(NacosConstants.ERROR_CONFIG_DELETE_FAILED, "返回结果不是 true");
            }
        } catch (Exception e) {
            handleFailure(NacosConstants.ERROR_CONFIG_DELETE_FAILED, e);
        }
    }
}