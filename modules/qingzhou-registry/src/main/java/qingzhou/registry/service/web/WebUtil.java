package qingzhou.registry.service.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.Registry;

class WebUtil {
    static final String REQUEST_PARAMETER_NAME_CACHE_KEY = "cache_key";

    static final String INSTANCE_ID = "instanceId";
    static final String APP_CODE = "appCode";
    static final String MODEL_CODE = "modelCode";

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

    static String webResult(Registry registry, Json json, HttpRequest httpRequest, Function<Context, Object> function) throws Exception {
        Object result = function.apply(httpRequest::getParameter);
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("data", result);
        metaData.put(REQUEST_PARAMETER_NAME_CACHE_KEY, registry.getDataTimestamp());
        return json.toJson(metaData);
    }
}
