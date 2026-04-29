package qingzhou.web.backend;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=" + WebBackendHttpHandler.URI_SERVER_PATH)
public class WebBackendHttpHandler implements HttpHandler {
    public static final String URI_SERVER_PATH = "/web";
    public static final String REQUEST_PARAMETER_NAME_CACHE_KEY = "cache_key";

    private static final ThreadLocal<HttpRequest> HTTP_REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    @Reference
    private Logger logger;

    @Reference
    private Json json;

    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    private final Map<String, WebHandler> handlerMap = new HashMap<>();

    @Activate
    public void init() {
        ContextHelper helper = new ContextHelper() {
            @Override
            public Registry getRegistry() {
                return registry;
            }

            @Override
            public String getI18n(String[] i18n, Object... args) {
                HttpRequest httpRequest = HTTP_REQUEST_THREAD_LOCAL.get();
                String langParam = null;
                if (httpRequest != null) langParam = httpRequest.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
                String i18nValue = i18nService.getI18n(i18n, langParam, args);
                return i18nValue == null ? "" : i18nValue;
            }

            @Override
            public String getParameter(String name) {
                HttpRequest httpRequest = HTTP_REQUEST_THREAD_LOCAL.get();
                if (httpRequest != null) {
                    return httpRequest.getParameter(name);
                }
                return null;
            }
        };

        handlerMap.put("welcome", new WelcomeInfo(helper));
        handlerMap.put("app", new AppMetaInfo(helper));
        handlerMap.put("model", new ModelMetaInfo(helper));
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // 查找
        WebHandler handler = null;
        String requestPath = httpRequest.getPath();
        String uriPrefix = URI_SERVER_PATH + "/";
        if (requestPath.startsWith(uriPrefix)) {
            String flag = requestPath.substring(uriPrefix.length());
            handler = handlerMap.get(flag);
        }
        if (handler == null) {
            httpResponse.statusBad();
            return;
        }

        // 是否已缓存
        String cacheKey = httpRequest.getParameter(REQUEST_PARAMETER_NAME_CACHE_KEY);
        if (cacheKey != null) {
            try {
                long cachedTime = Long.parseLong(cacheKey);
                if (cachedTime == registry.getDataTimestamp()) {
                    httpResponse.sendResponse("{\"success\":true}");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 执行
        HTTP_REQUEST_THREAD_LOCAL.set(httpRequest);
        try {
            Object result = handler.handle();
            if (result != null) {
                Map<String, Object> metaData = new HashMap<>();
                metaData.put("data", result);
                metaData.put(REQUEST_PARAMETER_NAME_CACHE_KEY, registry.getDataTimestamp());
                String jsonString = json.toJson(metaData);
                httpResponse.sendResponse(jsonString);
            }
        } catch (Exception e) {
            httpResponse.statusError();
            logger.error(e.getMessage(), e);
        } finally {
            HTTP_REQUEST_THREAD_LOCAL.remove();
        }
    }

    public interface WebHandler {
        Object handle();
    }

    public interface ContextHelper {
        Registry getRegistry();

        String getI18n(String[] i18n, Object... args);

        String getParameter(String name);
    }
}
