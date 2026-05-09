package qingzhou.registry.service.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;
import qingzhou.registry.service.llm.HandlingContext;

class WebUtil {
    static final String REQUEST_PARAMETER_NAME_CACHE_KEY = "cache_key";
    static final String REQUEST_PARAMETER_NAME_APP_ID = "appId";

    static String toAppId(String instanceId, String appCode) {
        return appCode + "@" + instanceId;
    }

    static String[] fromAppId(String appId) {
        String[] split = appId.split("@");
        if (split.length != 2) return null;
        return new String[]{split[1], split[0]};
    }

    static String toModelId(String instanceId, String appCode, String modelCode) {
        return modelCode + "@" + appCode + "@" + instanceId;
    }

    static String[] fromModelId(String modelId) {
        String[] split = modelId.split("@");
        if (split.length != 3) return null;
        return new String[]{split[2], split[1], split[0]};
    }

    static boolean cached(HttpRequest httpRequest, HttpResponse httpResponse, Registry registry) {
        String cacheKey = httpRequest.getParameter(REQUEST_PARAMETER_NAME_CACHE_KEY);
        if (cacheKey != null) {
            try {
                long cachedTime = Long.parseLong(cacheKey);
                if (cachedTime == registry.getDataTimestamp()) {
                    httpResponse.sendFinish("{\"success\":true}");
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    static String webResult(Registry registry, Json json, HttpRequest httpRequest, Function<HandlingContext, Object> function) throws Exception {
        Object result = function.apply(httpRequest::getParameter);
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("data", result);
        metaData.put(REQUEST_PARAMETER_NAME_CACHE_KEY, registry.getDataTimestamp());
        return json.toJson(metaData);
    }
}
