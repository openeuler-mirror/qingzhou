package qingzhou.app.nacos;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.json.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nacos Open API 封装：统一拼装 accessToken、构造请求与解析响应。
 *
 * <p>登录接口不走通用请求方法（{@link #buildUrl} 会触发取 token），以免递归。
 * 读操作返回已解析的 Map/List，写操作返回是否成功，由模型层负责展示与报错。
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

    public synchronized String getAccessToken() {
        if (accessToken == null) {
            try {
                Map<String, String> params = params("username", username, "password", password);
                Response result = httpClient.send(httpClient.buildRequest(serverAddr + NacosConstants.AUTH_LOGIN_PATH)
                        .method(HttpMethod.POST).params(params));
                if (result.getStatus() == 200) {
                    Map<String, Object> response = parse(result, Map.class);
                    accessToken = (String) response.get("accessToken");
                }
            } catch (Exception e) {
                // 认证不可用时保持 token 为空，后续请求会以无 token 失败并返回给上层
                accessToken = null;
            }
        }
        return accessToken;
    }

    // ==================== 配置管理 ====================

    public Map<String, Object> getConfigs(int pageNo, int pageSize, String tenant) throws Exception {
        Map<String, String> query = params("search", "accurate", "dataId", "", "group", "",
                "tenant", emptyIfNull(tenant), "pageNo", pageNo, "pageSize", pageSize);
        return parse(httpGet(NacosConstants.CONFIGS_PATH, query), Map.class);
    }

    public String getConfigContent(String dataId, String group, String tenant) throws Exception {
        return getBodyString(httpGet(NacosConstants.CONFIGS_PATH, configQuery(dataId, group, tenant)));
    }

    public Map<String, Object> getConfigInfo(String dataId, String group, String tenant) throws Exception {
        Map<String, String> query = configQuery(dataId, group, tenant);
        query.put("show", "all");
        return parse(httpGet(NacosConstants.CONFIGS_PATH, query), Map.class);
    }

    public boolean publishConfig(String dataId, String group, String content, String type, String tenant) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dataId", dataId);
        body.put("group", group);
        body.put("content", content);
        body.put("type", type);
        // public 命名空间不传 tenant，Nacos 才按默认空间处理
        if (tenant != null && !tenant.isEmpty()) body.put("tenant", tenant);
        return isSuccess(httpWrite(NacosConstants.CONFIGS_PATH, body, HttpMethod.POST));
    }

    public boolean deleteConfig(String dataId, String group, String tenant) throws Exception {
        return isSuccess(httpDelete(NacosConstants.CONFIGS_PATH, configQuery(dataId, group, tenant)));
    }

    // ==================== 服务管理 ====================

    public Map<String, Object> getServiceList(int pageNo, int pageSize) throws Exception {
        return parse(httpGet(NacosConstants.SERVICE_LIST_PATH, params("pageNo", pageNo, "pageSize", pageSize)), Map.class);
    }

    public Map<String, Object> getServiceInstances(String serviceName) throws Exception {
        return parse(httpGet(NacosConstants.INSTANCE_LIST_PATH, params("serviceName", serviceName)), Map.class);
    }

    public boolean registerInstance(String serviceName, String ip, int port, double weight,
                                    String namespaceId, String clusterName) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("serviceName", serviceName);
        body.put("ip", ip);
        body.put("port", port);
        body.put("weight", weight);
        body.put("namespaceId", namespaceId != null ? namespaceId : NacosConstants.PUBLIC_NAMESPACE);
        body.put("clusterName", clusterName != null ? clusterName : NacosConstants.DEFAULT_CLUSTER);
        body.put("healthy", "true");
        body.put("enabled", "true");
        return httpWrite(NacosConstants.INSTANCE_PATH, body, HttpMethod.POST).getStatus() == 200;
    }

    public boolean deleteService(String serviceName) throws Exception {
        return isSuccess(httpDelete(NacosConstants.SERVICE_PATH, params("serviceName", serviceName)));
    }

    // ==================== 命名空间管理 ====================

    @SuppressWarnings("unchecked")
    public List<Object> getNamespaces() throws Exception {
        Response result = httpGet(NacosConstants.NAMESPACES_PATH, null);
        try {
            return parse(result, List.class);
        } catch (Exception e) {
            // 契约适配：部分 Nacos 版本把列表包在 {"data":[...]} 中
            Map<String, Object> data = parse(result, Map.class);
            if (data != null && data.get("data") instanceof List) {
                return (List<Object>) data.get("data");
            }
            return null;
        }
    }

    public boolean createNamespace(String namespaceId, String namespaceName, String namespaceDesc) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("customNamespaceId", namespaceId != null ? namespaceId : "");
        body.put("namespaceName", namespaceName);
        body.put("namespaceDesc", namespaceDesc != null ? namespaceDesc : "");
        return isSuccess(httpWrite(NacosConstants.NAMESPACES_PATH, body, HttpMethod.POST));
    }

    public boolean updateNamespace(String namespaceId, String namespaceName, String namespaceDesc) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("namespace", namespaceId);
        body.put("namespaceShowName", namespaceName);
        body.put("namespaceDesc", namespaceDesc != null ? namespaceDesc : "");
        return isSuccess(httpWrite(NacosConstants.NAMESPACES_PATH, body, HttpMethod.PUT));
    }

    public boolean deleteNamespace(String namespaceId) throws Exception {
        return isSuccess(httpDelete(NacosConstants.NAMESPACES_PATH, params("namespaceId", namespaceId)));
    }

    // ==================== HTTP 与解析基础 ====================

    /** 构造查询/表单参数：键为偶数位、值为奇数位，值按 String.valueOf 转换（与 URL 拼接行为一致）。 */
    private static Map<String, String> params(Object... kv) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            result.put(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        }
        return result;
    }

    /** 配置类接口的三元定位参数 dataId + group + tenant；public 命名空间以空串表示。 */
    private static Map<String, String> configQuery(String dataId, String group, String tenant) {
        return params("dataId", dataId, "group", group, "tenant", emptyIfNull(tenant));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private String buildUrl(String path, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(serverAddr + path).append("?accessToken=").append(getAccessToken());
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return url.toString();
    }

    private Response httpGet(String path, Map<String, String> queryParams) throws Exception {
        return httpClient.send(httpClient.buildRequest(buildUrl(path, queryParams)).method(HttpMethod.GET));
    }

    private Response httpWrite(String path, Map<String, Object> body, HttpMethod method) throws Exception {
        Map<String, String> stringParams = new HashMap<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            stringParams.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return httpClient.send(httpClient.buildRequest(buildUrl(path, null)).method(method).params(stringParams));
    }

    private Response httpDelete(String path, Map<String, String> queryParams) throws Exception {
        return httpClient.send(httpClient.buildRequest(buildUrl(path, queryParams)).method(HttpMethod.DELETE));
    }

    @SuppressWarnings("unchecked")
    private <T> T parse(Response result, Class<?> type) throws Exception {
        String body = getBodyString(result);
        if (body == null || body.isEmpty()) return null;
        Object parsed = json.fromJson(body, type);
        return (T) parsed;
    }

    private String getBodyString(Response result) {
        return result == null || result.getBody() == null ? null : new String(result.getBody());
    }

    private boolean isSuccess(Response result) {
        return result.getStatus() == 200 && "true".equals(getBodyString(result));
    }
}
