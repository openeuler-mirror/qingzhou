package qingzhou.app.nginx;

import java.io.IOException;
import java.util.*;
import qingzhou.api.*;

/**
 * Nginx配置管理模型 用于查看和修改nginx.conf中的常用配置项
 */
@Model(code = "settings", order = 3, icon = "Setting", menu = "basic",
        name = {"配置管理", "en:Settings"}, info = {"查看和修改Nginx常用配置", "en:View and modify Nginx settings"})
public class NginxSettings extends qingzhou.api.ModelBase implements qingzhou.api.type.List, qingzhou.api.type.Show, qingzhou.api.type.Update {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"配置项", "en:Directive"},
            info = {"配置项名称", "en:Configuration directive name"})
    public String directive;

    @ModelField(input_type = InputType.text, list = true, show = true, update = true, search = true,
            name = {"配置值", "en:Value"},
            info = {"配置值", "en:Configuration value"})
    public String value;

    @ModelField(list = true, show = true, readonly = true,
            name = {"描述", "en:Description"},
            info = {"配置项说明", "en:Configuration description"})
    public String description;

    @ModelField(list = true, show = true, readonly = true,
            name = {"配置区块", "en:Block"},
            info = {"配置项所属的区块", "en:Configuration block"})
    public String block;

    /**
     * 列表查询：返回分页的配置列表
     */
    @Override
    public java.util.List<String[]> list(Request request, int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> allConfigs = getAllConfigs();

        // 过滤
        java.util.List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> config : allConfigs) {
            if (matchesQuery(config, query)) {
                filtered.add(config);
            }
        }

        // 分页
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> config = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String fieldName = listFields[j];
                String fieldValue = config.get(fieldName);
                data[j] = fieldValue != null ? fieldValue : "";
            }
            result.add(data);
        }

        return result;
    }

    /**
     * 检查配置项是否存在
     */
    @Override
    public boolean contains(String id) {
        try {
            Map<String, String> configs = NginxConfigParser.parseConfig();
            return configs.containsKey(id);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 总数查询
     */
    @Override
    public int totalSize(Map<String, String> query) {
        try {
            java.util.List<Map<String, String>> allConfigs = getAllConfigs();
            int count = 0;
            for (Map<String, String> config : allConfigs) {
                if (matchesQuery(config, query)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 查看详情
     */
    @Override
    public Map<String, String> show(Request request) throws Exception {
        String directiveName = request.getId();
        Map<String, String> configs = NginxConfigParser.parseConfig();
        String value = configs.get(directiveName);

        if (value == null) {
            return null;
        }

        Map<String, String> descriptions = NginxConfigParser.getConfigDescriptions();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("directive", directiveName);
        result.put("value", value);
        result.put("description", descriptions.getOrDefault(directiveName, ""));
        result.put("block", getBlockName(directiveName));

        return result;
    }

    /**
     * 更新配置
     */
    @Override
    public void update(Request request, Map<String, String> data) throws Exception {
        String directiveName = request.getId();
        String newValue = data.get("value");

        if (newValue == null || newValue.trim().isEmpty()) {
            throw new IllegalArgumentException("配置值不能为空");
        }

        // 更新配置文件
        NginxConfigParser.updateConfig(directiveName, newValue.trim());
    }

    /**
     * 获取所有配置项
     */
    private java.util.List<Map<String, String>> getAllConfigs() throws IOException {
        Map<String, String> configs = NginxConfigParser.parseConfig();
        Map<String, String> descriptions = NginxConfigParser.getConfigDescriptions();

        java.util.List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            Map<String, String> configMap = new LinkedHashMap<>();
            configMap.put("directive", entry.getKey());
            configMap.put("value", entry.getValue());
            configMap.put("description", descriptions.getOrDefault(entry.getKey(), ""));
            configMap.put("block", getBlockName(entry.getKey()));
            result.add(configMap);
        }

        return result;
    }

    /**
     * 查询匹配
     */
    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.trim().isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取配置项所属区块
     */
    private String getBlockName(String directive) {
        return NginxConfigParser.getConfigBlock(directive);
    }
}
