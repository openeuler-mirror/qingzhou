package qingzhou.app.nacos;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nacos API 统一封装类
 * 提供所有 Nacos HTTP API 的统一调用接口
 */
public class NacosApi {
    
    private final HttpClient httpClient;
    private final Json json;
    private final String serverAddr;
    private final String username;
    private final String password;
    private String accessToken;
    
    public NacosApi(HttpClient httpClient, Json json, String serverAddr, String username, String password) {
        this.httpClient = httpClient;
        this.json = json;
        this.serverAddr = serverAddr;
        this.username = username;
        this.password = password;
    }
    
    /**
     * 获取访问令牌
     */
    public synchronized String getAccessToken() {
        if (accessToken == null) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                
                // 直接调用认证 API，不使用通用方法（避免递归）
                String url = buildAuthUrl();
                Map<String, String> stringParams = convertToStringParams(params);
                Response result = httpClient.send(httpClient.newRequest(url).method(HttpMethod.POST).params(stringParams));
                
                if (result.getStatus() == 200) {
                    Map<String, Object> response = parseObject(result);
                    accessToken = (String) response.get("accessToken");
                }
            } catch (Exception e) {
                accessToken = null;
            }
        }
        return accessToken;
    }
    
    /**
     * 刷新访问令牌
     */
    public synchronized void refreshAccessToken() {
        accessToken = null;
        getAccessToken();
    }
    
    // ==================== 配置管理 API ====================
    
    /**
     * 获取配置列表
     */
    public Map<String, Object> getConfigs(int pageNo, int pageSize, String tenant) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("search", "accurate");
        queryParams.put("dataId", "");
        queryParams.put("group", "");
        queryParams.put("tenant", tenant != null ? tenant : "");
        queryParams.put("pageNo", String.valueOf(pageNo));
        queryParams.put("pageSize", String.valueOf(pageSize));
        
        return parseObject(httpGet(NacosConstants.CONFIGS_PATH, queryParams));
    }
    
    /**
     * 获取配置内容
     */
    public String getConfigContent(String dataId, String group, String tenant) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("dataId", dataId);
        queryParams.put("group", group);
        queryParams.put("tenant", tenant != null ? tenant : "");
        
        return getBodyString(httpGet(NacosConstants.CONFIGS_PATH, queryParams));
    }
    
    /**
     * 获取配置详情
     */
    public Map<String, Object> getConfigInfo(String dataId, String group, String tenant) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("show", "all");
        queryParams.put("dataId", dataId);
        queryParams.put("group", group);
        queryParams.put("tenant", tenant != null ? tenant : "");
        
        return parseObject(httpGet(NacosConstants.CONFIGS_PATH, queryParams));
    }
    
    /**
     * 发布配置
     */
    public boolean publishConfig(String dataId, String group, String content, String type, String tenant) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("dataId", dataId);
        params.put("group", group);
        params.put("content", content);
        params.put("type", type);
        if (tenant != null && !tenant.isEmpty()) {
            params.put("tenant", tenant);
        }
        
        Response result = httpPost(NacosConstants.CONFIGS_PATH, params);
        return isSuccess(result);
    }
    
    /**
     * 删除配置
     */
    public boolean deleteConfig(String dataId, String group, String tenant) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("dataId", dataId);
        queryParams.put("group", group);
        queryParams.put("tenant", tenant != null ? tenant : "");
        
        return isSuccess(httpDelete(NacosConstants.CONFIGS_PATH, queryParams));
    }
    
    // ==================== 服务管理 API ====================
    
    /**
     * 获取服务列表
     */
    public Map<String, Object> getServiceList(int pageNo, int pageSize) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("pageNo", String.valueOf(pageNo));
        queryParams.put("pageSize", String.valueOf(pageSize));
        
        return parseObject(httpGet(NacosConstants.SERVICE_LIST_PATH, queryParams));
    }
    
    /**
     * 获取服务实例列表
     */
    public Map<String, Object> getServiceInstances(String serviceName) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("serviceName", serviceName);
        
        return parseObject(httpGet(NacosConstants.INSTANCE_LIST_PATH, queryParams));
    }
    
    /**
     * 注册服务实例
     */
    public boolean registerInstance(String serviceName, String ip, int port, double weight,
                                    String namespaceId, String clusterName) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("serviceName", serviceName);
        params.put("ip", ip);
        params.put("port", port);
        params.put("weight", weight);
        params.put("namespaceId", namespaceId != null ? namespaceId : NacosConstants.PUBLIC_NAMESPACE);
        params.put("clusterName", clusterName != null ? clusterName : NacosConstants.DEFAULT_CLUSTER);
        params.put("healthy", "true");
        params.put("enabled", "true");
        
        Response result = httpPost(NacosConstants.INSTANCE_PATH, params);
        return result.getStatus() == 200;
    }
    
    /**
     * 删除服务
     */
    public boolean deleteService(String serviceName) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("serviceName", serviceName);
        
        return isSuccess(httpDelete(NacosConstants.SERVICE_PATH, queryParams));
    }
    
    // ==================== 命名空间管理 API ====================
    
    /**
     * 获取命名空间列表
     */
    public List<Object> getNamespaces() throws Exception {
        Response result = httpGet(NacosConstants.NAMESPACES_PATH, null);
        try {
            return parseArray(result);
        } catch (Exception e) {
            // 某些版本返回格式不同，尝试解析为对象
            Map<String, Object> data = parseObject(result);
            if (data != null && data.containsKey("data")) {
                Object dataObj = data.get("data");
                if (dataObj instanceof List) {
                    return (List<Object>) dataObj;
                }
            }
            return null;
        }
    }
    
    /**
     * 创建命名空间
     */
    public boolean createNamespace(String namespaceId, String namespaceName, String namespaceDesc) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("customNamespaceId", namespaceId != null ? namespaceId : "");
        params.put("namespaceName", namespaceName);
        params.put("namespaceDesc", namespaceDesc != null ? namespaceDesc : "");
        
        Response result = httpPost(NacosConstants.NAMESPACES_PATH, params);
        return isSuccess(result);
    }
    
    /**
     * 更新命名空间
     */
    public boolean updateNamespace(String namespaceId, String namespaceName, String namespaceDesc) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("namespace", namespaceId);
        params.put("namespaceShowName", namespaceName);
        params.put("namespaceDesc", namespaceDesc != null ? namespaceDesc : "");
        
        Response result = httpPut(NacosConstants.NAMESPACES_PATH, params);
        return isSuccess(result);
    }
    
    /**
     * 删除命名空间
     */
    public boolean deleteNamespace(String namespaceId) throws Exception {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("namespaceId", namespaceId);
        
        return isSuccess(httpDelete(NacosConstants.NAMESPACES_PATH, queryParams));
    }
    
    // ==================== HTTP 请求基础方法 ====================
    
    /**
     * 构建完整的 URL（包含 accessToken）
     */
    private String buildUrl(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(serverAddr + path);
        url.append("?accessToken=").append(getAccessToken());
        
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        
        return url.toString();
    }
    
    /**
     * 构建认证 URL（不包含 accessToken）
     */
    private String buildAuthUrl() {
        return serverAddr + NacosConstants.AUTH_LOGIN_PATH;
    }
    
    /**
     * GET 请求
     */
    private Response httpGet(String path, Map<String, String> queryParams) throws Exception {
        String url = buildUrl(path, queryParams);
        return httpClient.send(httpClient.newRequest(url).method(HttpMethod.GET));
    }
    
    /**
     * POST 请求
     */
    private Response httpPost(String path, Map<String, Object> params) throws Exception {
        String url = buildUrl(path, null);
        Map<String, String> stringParams = convertToStringParams(params);
        return httpClient.send(httpClient.newRequest(url).method(HttpMethod.POST).params(stringParams));
    }
    
    /**
     * PUT 请求
     */
    private Response httpPut(String path, Map<String, Object> params) throws Exception {
        String url = buildUrl(path, null);
        Map<String, String> stringParams = convertToStringParams(params);
        return httpClient.send(httpClient.newRequest(url).method(HttpMethod.PUT).params(stringParams));
    }
    
    /**
     * DELETE 请求
     */
    private Response httpDelete(String path, Map<String, String> queryParams) throws Exception {
        String url = buildUrl(path, queryParams);
        return httpClient.send(httpClient.newRequest(url).method(HttpMethod.DELETE));
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 转换参数为字符串类型
     */
    private Map<String, String> convertToStringParams(Map<String, Object> params) {
        Map<String, String> stringParams = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                stringParams.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return stringParams;
    }
    
    /**
     * 解析响应为 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObject(Response result) throws Exception {
        if (result == null || result.getBody() == null) {
            return null;
        }
        String body = getBodyString(result);
        if (body.isEmpty()) {
            return null;
        }
        return json.fromJson(body, Map.class);
    }
    
    /**
     * 解析响应为 List
     */
    @SuppressWarnings("unchecked")
    private List<Object> parseArray(Response result) throws Exception {
        if (result == null || result.getBody() == null) {
            return null;
        }
        String body = getBodyString(result);
        if (body.isEmpty()) {
            return null;
        }
        return json.fromJson(body, List.class);
    }
    
    /**
     * 获取响应体字符串
     */
    private String getBodyString(Response result) {
        if (result == null || result.getBody() == null) {
            return null;
        }
        return new String(result.getBody());
    }
    
    /**
     * 检查操作是否成功
     */
    private boolean isSuccess(Response result) {
        return result.getStatus() == 200 && "true".equals(getBodyString(result));
    }
}