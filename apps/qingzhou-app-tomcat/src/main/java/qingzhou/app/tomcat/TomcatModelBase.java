package qingzhou.app.tomcat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import qingzhou.api.ModelBase;
import qingzhou.xml.Doc;
import qingzhou.xml.Xml;

public class TomcatModelBase extends ModelBase {

    public String getTomcatBaseDir() {
        String basePath = getAppContext().getDetectedPath();
        if (basePath == null || basePath.isEmpty()) {
            System.err.println("tomcat path is empty.");
            return null;
        }
        return basePath;
    }

    public String getBaseLogsDir() {
        String basePath = getTomcatBaseDir();
        if (basePath == null) {
            return null;
        }
        return resolvePath(basePath, "logs").toString();
    }

    public File getContextXmlFile() {
        String basePath = getTomcatBaseDir();
        if (basePath == null) {
            return null;
        }
        return resolvePath(basePath, "conf", "context.xml").toFile();
    }

    public File getServerXmlFile() {
        String basePath = getTomcatBaseDir();
        if (basePath == null) {
            return null;
        }
        return resolvePath(basePath, "conf", "server.xml").toFile();
    }

    /**
     * 解析路径，使用 Path API 确保跨平台兼容性
     */
    protected Path resolvePath(String basePath, String... subPaths) {
        Path path = Paths.get(basePath);
        for (String subPath : subPaths) {
            path = path.resolve(subPath);
        }
        return path;
    }

    /**
     * 通用的查询过滤方法
     */
    protected List<Map<String, String>> filterByQuery(List<Map<String, String>> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return data;
        }
        return data.stream()
                .filter(item -> matchesQuery(item, query))
                .collect(Collectors.toList());
    }

    /**
     * 检查单个数据项是否匹配查询条件
     */
    private boolean matchesQuery(Map<String, String> item, Map<String, String> query) {
        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String queryValue = entry.getValue();
            if (queryValue != null && !queryValue.trim().isEmpty()) {
                String itemValue = item.get(key);
                if (itemValue == null || !itemValue.toLowerCase().contains(queryValue.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 通用的列表结果构建方法
     */
    protected List<String[]> buildListResult(List<Map<String, String>> data, int pageNum, int pageSize, String[] listFields) {
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, data.size());

        List<String[]> result = new ArrayList<>();
        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> item = data.get(i);
            String[] row = Arrays.stream(listFields)
                    .map(field -> item.getOrDefault(field, ""))
                    .toArray(String[]::new);
            result.add(row);
        }
        return result;
    }
    
    // ==================== XML 操作方法 ====================
    
    /**
     * 获取 Xml 服务
     */
    protected Xml getXmlService() {
        return getAppContext().getService(Xml.class);
    }
    
    /**
     * 解析 XML 文件
     */
    protected Doc parseXml(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalStateException("XML 文件不存在: " + (file != null ? file.getPath() : "null"));
        }
        return getXmlService().parse(file);
    }
    
    /**
     * 查询 XML 节点列表
     */
    protected List<Properties> getXmlNodes(File file, String xpath) throws Exception {
        Doc doc = parseXml(file);
        return doc.getNodes(xpath);
    }
    
    /**
     * 查询 XML 单个节点
     */
    protected Properties getXmlNode(File file, String xpath) throws Exception {
        Doc doc = parseXml(file);
        return doc.getNode(xpath);
    }
    
    /**
     * 检查 XML 节点是否存在
     */
    protected boolean xmlNodeExists(File file, String xpath) {
        try {
            Properties node = getXmlNode(file, xpath);
            return node != null && !node.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 添加 XML 节点
     */
    protected void addXmlNode(File file, String parentPath, String nodeName, Properties attrs) throws Exception {
        Doc doc = parseXml(file);
        doc.addNode(parentPath, nodeName, attrs);
        doc.write(file);
    }
    
    /**
     * 更新 XML 节点
     */
    protected void updateXmlNode(File file, String xpath, Properties attrs) throws Exception {
        Doc doc = parseXml(file);
        doc.updateNode(xpath, attrs);
        doc.write(file);
    }
    
    /**
     * 删除 XML 节点
     */
    protected void deleteXmlNode(File file, String xpath) throws Exception {
        Doc doc = parseXml(file);
        doc.deleteNode(xpath);
        doc.write(file);
    }

    /**
     * Properties 转 Map
     */
    protected Map<String, String> toMap(Properties props) {
        Map<String, String> map = new java.util.HashMap<>();
        if (props != null) {
            for (String key : props.stringPropertyNames()) {
                map.put(key, props.getProperty(key));
            }
        }
        return map;
    }
}
