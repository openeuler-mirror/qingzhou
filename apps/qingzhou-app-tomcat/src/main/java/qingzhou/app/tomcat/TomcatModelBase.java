package qingzhou.app.tomcat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.logger.Logger;

/** Tomcat 模型基类：共享路径解析、监控 id 兜底、列表过滤/分页与日志等辅助。 */
public class TomcatModelBase extends ModelBase {

    public String getTomcatBaseDir() {
        String basePath = getAppContext().getDetectedPath();
        if (basePath == null || basePath.isEmpty()) {
            // 无探测路径时不能返回 null：后续会退化为"匹配任意 Tomcat 进程"，可能连错实例
            throw new IllegalStateException("tomcat path is empty");
        }
        return basePath;
    }

    public String getBaseLogsDir() {
        return resolvePath(getTomcatBaseDir(), "logs").toString();
    }

    /** 从 Context path 提取应用名称："/demo" -> "demo"，空路径 -> ROOT。 */
    static String appName(String path) {
        if (path == null || path.trim().isEmpty()) return "ROOT";
        String name = path.trim();
        if (name.startsWith("/")) name = name.substring(1);
        return name.isEmpty() ? "ROOT" : name;
    }

    /** 监视动作由前端以 POST /{model}/monitor + body{id} 调用，路径中无 id，需从参数兜底。 */
    static String monitorId(Request request) {
        String id = request.getId();
        if (id == null || id.isEmpty()) id = request.getParameter("id");
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("缺少记录 id 参数");
        return id;
    }

    protected Path resolvePath(String basePath, String... subPaths) {
        Path path = Paths.get(basePath);
        for (String subPath : subPaths) path = path.resolve(subPath);
        return path;
    }

    /** 仅对非空查询值做大小写不敏感的包含匹配。 */
    protected List<Map<String, String>> filterByQuery(List<Map<String, String>> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return data;
        return data.stream().filter(item -> matchesQuery(item, query)).collect(Collectors.toList());
    }

    private boolean matchesQuery(Map<String, String> item, Map<String, String> query) {
        for (Map.Entry<String, String> entry : query.entrySet()) {
            String queryValue = entry.getValue();
            if (queryValue != null && !queryValue.trim().isEmpty()) {
                String itemValue = item.get(entry.getKey());
                if (itemValue == null || !itemValue.toLowerCase().contains(queryValue.toLowerCase())) return false;
            }
        }
        return true;
    }

    /** 按页截取数据集并投影为 listFields 对应的字符串行。 */
    protected List<String[]> buildListResult(List<Map<String, String>> data, int pageNum, int pageSize, String[] listFields) {
        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, data.size());
        List<String[]> result = new ArrayList<>();
        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> item = data.get(i);
            result.add(Arrays.stream(listFields).map(field -> item.getOrDefault(field, "")).toArray(String[]::new));
        }
        return result;
    }

    protected void logError(String message, Throwable e) {
        Logger logger = getAppContext().getService(Logger.class);
        if (logger != null) logger.error(message, e);
    }
}
