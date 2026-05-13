package qingzhou.web;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

/**
 * 静态资源服务，提供前端 UI 的静态文件访问
 */
@Component(property = HttpHandler.HANDLE_PATH + "=")
public class StaticResourceHandler implements HttpHandler {

    private static final String STATIC_RESOURCE_PATH = "/webapp/";
    private static final String INDEX_FILE = "index.html";
    // 缓存配置
    private final Map<String, WebResource> resources = new ConcurrentHashMap<>();

    // MIME 类型映射
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("otf", "font/otf");
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        String requestPath = httpRequest.getPath();

        // 去掉 /console 前缀
        if (requestPath.startsWith("/console")) {
            requestPath = requestPath.substring("/console".length());
            if (requestPath.isEmpty()) {
                requestPath = "/";
            }
        }

        // 处理静态资源
        serveStaticResource(httpRequest, requestPath, httpResponse);
    }

    /**
     * 提供静态资源
     */
    private void serveStaticResource(HttpRequest httpRequest, String path, HttpResponse response) {
        // 规范化路径
        if (path.equals("/") || path.isEmpty()) {
            path = "/" + INDEX_FILE;
        }

        // 移除开头的斜杠
        String resourcePath = STATIC_RESOURCE_PATH + path.substring(1);

        WebResource webResource = resources.computeIfAbsent(resourcePath, s -> {
            URL resource = getClass().getResource(s);
            if (resource == null) {
                return null;
            }
            return new WebResource(s, resource);
        });
        if (webResource == null) {
            if (!path.endsWith(INDEX_FILE)) {
                serveStaticResource(httpRequest, "/" + INDEX_FILE, response);
                return;
            }
            response.status404Finish();
            return;
        }

        String cacheControl = getCacheControl(path);
        response.header("Cache-Control", cacheControl);

        try {
            // 生成 ETag（MD5）
            String etag = webResource.getETag();

            // 协商缓存校验：If-None-Match
            boolean isEtagMatch = checkIfNoneMatch(httpRequest, etag);
            response.header("ETag", etag);

            // 辅助缓存头
            response.header("Vary", "Accept-Encoding, Accept");

            // 资源未修改 → 直接返回 304
            if (isEtagMatch) {
                response.status(304);
                return;
            }

            // 设置 Content-Type
            String contentType = getContentType(path);
            response.contentType(contentType);

            // 读取并发送文件内容
            response.sendFinish(webResource.getContent());
        } catch (Exception e) {
            response.status500Finish("internal server error");
        }
    }

    private static String getCacheControl(String path) {
        String cacheControl;
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            cacheControl = "max-age=0, no-cache, must-revalidate, proxy-revalidate, no-transform";
        } else if (path.endsWith(".svg")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".png")
                || path.endsWith(".gif")
                || path.endsWith(".ico")) {
            // 图片设置一个月缓存
            cacheControl = String.format(
                    "max-age=%d, must-revalidate, proxy-revalidate, no-transform",
                    30 * 24 * 60 * 60
            );
        } else {
            // 当前assets目录下的文件名自带hash值，可以设置长期缓存
            cacheControl = String.format(
                    "max-age=%d, must-revalidate, proxy-revalidate, no-transform",
                    30 * 24 * 60 * 60 * 365
            );
        }
        return cacheControl;
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = path.substring(lastDot + 1).toLowerCase();
            String mimeType = MIME_TYPES.get(extension);
            if (mimeType != null) {
                return mimeType;
            }
        }
        return "application/octet-stream";
    }

    /**
     * 校验 If-None-Match
     */
    private boolean checkIfNoneMatch(HttpRequest request, String etag) {
        String ifNoneMatch = request.getHeader("If-None-Match");
        return ifNoneMatch != null && (ifNoneMatch.equals(etag) || ifNoneMatch.equals("*"));
    }

}
