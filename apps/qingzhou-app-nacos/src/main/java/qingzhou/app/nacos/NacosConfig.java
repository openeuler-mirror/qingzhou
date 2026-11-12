package qingzhou.app.nacos;

import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.action.*;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "nacos-config", order = 3, icon = "Setting",
        name = {"配置列表", "en:Config Management"}, info = {"Nacos配置管理中心", "en:Nacos Configuration Management"})
public class NacosConfig extends NacosModelBase implements Page, Show, Add, Update, Delete {

    /** 列表行的 id 是 Nacos 内部序号，写操作需 dataId/group/tenant 定位，故按 id 缓存映射。 */
    private final Map<String, Map<String, String>> configCache = new HashMap<>();

    @ModelField(id = true, list = true, show = true, add = false, required = true, name = {"ID", "en:ID"})
    public String id;
    @ModelField(name = {"命名空间ID", "en:Namespace"}, list = true, show = false, add = true, update = false, search = true, required = true)
    public String namespace;
    @ModelField(name = {"Data Id", "en:Data ID"}, list = true, add = true, update = false, required = true)
    public String dataId;
    @ModelField(name = {"GROUP", "en:Group"}, list = true, show = true, add = true, update = true, required = true)
    public String group = NacosConstants.DEFAULT_GROUP;
    @ModelField(name = {"归属应用", "en:Belong App"}, list = true, show = false, add = false, update = false)
    public String belongApp;
    @ModelField(name = {"MD5", "en:MD5"}, add = false, update = false, show = true)
    public String md5;
    @ModelField(name = {"配置格式", "en:Type"}, list = true, show = false, add = true, update = true, input_type = InputType.select, options = {"text", "json", "xml", "yaml", "html", "properties"})
    public String type;
    @ModelField(name = {"内容", "en:Content"}, show = true, add = true, update = true, input_type = InputType.textarea, required = true)
    public String content;

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        try {
            String namespaceId = query != null ? query.get("namespace") : null;
            String tenant = getEffectiveNamespaceId(namespaceId);
            configCache.clear();
            Map<String, Object> data = getNacosApi().getConfigs(pageNum, pageSize, tenant);
            List<?> configs = (List<?>) data.get("pageItems");

            List<Map<String, String>> items = new ArrayList<>();
            if (configs != null) {
                for (Object configObj : configs) {
                    Map<String, Object> config = (Map<String, Object>) configObj;
                    String configId = String.valueOf(config.get("id"));
                    Map<String, String> info = new HashMap<>();
                    info.put("id", configId);
                    info.put("namespace", isPublicNamespace(namespaceId) ? NacosConstants.PUBLIC_NAMESPACE : namespaceId);
                    info.put("dataId", String.valueOf(config.get("dataId")));
                    info.put("group", String.valueOf(config.get("group")));
                    info.put("tenant", tenant);
                    info.put("type", String.valueOf(config.get("type")));
                    info.put("belongApp", String.valueOf(config.get("appName")));
                    configCache.put(configId, info);
                    items.add(info);
                }
            }
            return toRows(items, listFields);
        } catch (Exception e) {
            logError("Nacos config query failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int total(Map<String, String> query) {
        try {
            String tenant = getEffectiveNamespaceId(query != null ? query.get("namespace") : null);
            return ((Number) getNacosApi().getConfigs(1, 1, tenant).get("totalCount")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean contains(String id) {
        // Nacos 未开放按内部序号查询，写操作由服务端校验存在性
        return true;
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> result = new HashMap<>();
        Map<String, String> cached = cachedConfig(id);
        result.putAll(cached);
        String dataId = cached.getOrDefault("dataId", "");
        String group = cached.getOrDefault("group", "");
        String tenant = cached.getOrDefault("tenant", "");
        try {
            NacosApi nacosApi = getNacosApi();
            result.put("content", nacosApi.getConfigContent(dataId, group, tenant));
            Map<String, Object> configInfo = nacosApi.getConfigInfo(dataId, group, tenant);
            if (result.get("type") == null) result.put("type", String.valueOf(configInfo.get("type")));
            if (result.get("tenant") == null) result.put("tenant", String.valueOf(configInfo.get("tenant")));
            result.put("md5", String.valueOf(configInfo.get("md5")));
            result.put("belongApp", String.valueOf(configInfo.get("appName")));
        } catch (Exception e) {
            logError("Nacos config query failed", e);
        }
        return result;
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String dataId = data.get("dataId");
        String group = getOrDefault(data.get("group"), NacosConstants.DEFAULT_GROUP);
        String content = data.get("content");
        String type = getOrDefault(data.get("type"), NacosConstants.DEFAULT_CONFIG_TYPE);
        String tenant = getEffectiveNamespaceId(data.get("namespace"));
        writeOrFail(NacosConstants.ERROR_CONFIG_PUBLISH_FAILED, "返回结果不是 true",
                () -> getNacosApi().publishConfig(dataId, group, content, type, tenant));
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        Map<String, String> cached = cachedConfig(id);
        String content = data.get("content");
        String type = getOrDefault(data.get("type"), NacosConstants.DEFAULT_CONFIG_TYPE);
        writeOrFail(NacosConstants.ERROR_CONFIG_UPDATE_FAILED, "返回结果不是 true",
                () -> getNacosApi().publishConfig(cached.getOrDefault("dataId", ""), cached.getOrDefault("group", ""),
                        content, type, cached.getOrDefault("tenant", "")));
    }

    @Override
    public void delete(String id) throws Exception {
        Map<String, String> cached = cachedConfig(id);
        writeOrFail(NacosConstants.ERROR_CONFIG_DELETE_FAILED, "返回结果不是 true",
                () -> getNacosApi().deleteConfig(cached.getOrDefault("dataId", ""),
                        cached.getOrDefault("group", ""), cached.getOrDefault("tenant", "")));
    }

    /** 由列表行 id 取缓存的定位信息；未缓存时返回空 Map，各字段按空串处理。 */
    private Map<String, String> cachedConfig(String id) {
        Map<String, String> cached = configCache.get(decode(id));
        return cached != null ? cached : Collections.emptyMap();
    }
}
