package qingzhou.registry.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

/**
 * 元数据查询 handler 公共模板：缓存命中短路 + 统一响应格式。
 * 注入须写在具体组件类上：bnd 不处理父类字段上的 DS 注解，写在父类会导致字段为 null。
 */
abstract class AbstractMetaHandler implements HttpHandler {
    private static final String CACHE_KEY = "cache_key";

    protected Registry registry;
    protected Json json;

    abstract Function<RequestParameters, Object> getFunction();

    @Override
    public final void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        if (isCacheHit(httpRequest, httpResponse)) return; // 是否已缓存

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("data", getFunction().apply(httpRequest::getParameter));
        responseBody.put(CACHE_KEY, registry.getRegistryDataVersion());
        httpResponse.contentTypeJsonUtf8()
                .send(json.toJson(responseBody));
    }

    // cache_key 为客户端上次收到的数据版本，与当前版本一致即数据未变更
    private boolean isCacheHit(HttpRequest httpRequest, HttpResponse httpResponse) {
        String cacheKey = httpRequest.getParameter(CACHE_KEY);
        if (cacheKey == null) return false;
        try {
            if (Long.parseLong(cacheKey) != registry.getRegistryDataVersion()) return false;
            httpResponse.send("{\"success\":true}");
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
