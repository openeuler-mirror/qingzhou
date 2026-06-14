package qingzhou.web;

import java.net.URL;
import java.util.*;
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
    // 优化：图片扩展名集合，供 Cache-Control 判断使用，避免与 MIME 判断各维护一套 endsWith 硬编码
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("png", "jpg", "jpeg", "gif", "svg", "ico"));

    // 优化：Cache-Control 取值是固定常量，预先拼好，避免每次请求 String.format 重新拼接
    private static final String CACHE_CONTROL_NO_CACHE =
            "max-age=0, no-cache, must-revalidate, proxy-revalidate, no-transform";
    // 图片缓存一个月
    private static final String CACHE_CONTROL_IMAGE =
            "max-age=" + (30 * 24 * 60 * 60) + ", must-revalidate, proxy-revalidate, no-transform";
    // 修复：assets 文件名自带 hash，可长期缓存。原值 30*24*60*60*365 实为约 30 年（把"一个月秒数"又乘了 365），
    // 语义错乱，更正为 1 年 = 365*24*60*60 秒
    private static final String CACHE_CONTROL_LONG =
            "max-age=" + (365 * 24 * 60 * 60) + ", must-revalidate, proxy-revalidate, no-transform";

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

        // 修复：仅当路径恰好是 "/web" 或以 "/web/" 开头时才剥离前缀，
        // 避免把 "/website" 等以 "web" 开头的合法路径误当作带 "/web" 前缀而错误截断
        if (requestPath.equals("/web")) {
            requestPath = "/";
        } else if (requestPath.startsWith("/web/")) {
            requestPath = requestPath.substring("/web".length());
        }

        // 安全修复（路径穿越）：getResource 在目录型 ClassLoader 下会解析 ".."，
        // 形如 /web/../../x 的请求可能读取到 webapp/ 之外的资源，故含非法字符的路径直接拒绝
        if (!isPathSafe(requestPath)) {
            httpResponse.status404Finish();
            return;
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

        // 优化：扩展名只解析一次，供 Cache-Control 与 Content-Type 共用
        String extension = getExtension(path);

        response.header("Cache-Control", getCacheControl(extension));

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
                // 修复：status(int) 是 builder 风格、不会结束响应，原代码漏调 finish() 导致 304 响应未真正提交
                response.status(304)
                        .finish();
                return;
            }

            // 设置 Content-Type
            response.contentType(getContentType(extension));

            // 读取并发送文件内容
            response.sendFinish(webResource.getContent());
        } catch (Exception e) {
            response.status500Finish("internal server error");
        }
    }

    /**
     * 安全校验：拒绝包含路径穿越段（..）、反斜杠或空字节的路径。
     * 按 "/" 分段比较，避免误伤文件名中合法的连续点（如 a..b.js）
     */
    private static boolean isPathSafe(String path) {
        if (path.indexOf('\0') >= 0 || path.indexOf('\\') >= 0) {
            return false;
        }
        for (String segment : path.split("/")) {
            if (segment.equals("..")) {
                return false;
            }
        }
        return true;
    }

    /**
     * 提取文件扩展名（小写），无扩展名时返回空串
     */
    private static String getExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        // 点必须位于最后一段文件名内，且不是文件名首字符（排除隐藏文件如 .gitignore）
        if (lastDot > lastSlash + 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static String getCacheControl(String extension) {
        if (extension.equals("html") || extension.equals("htm")) {
            return CACHE_CONTROL_NO_CACHE;
        }
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return CACHE_CONTROL_IMAGE;
        }
        return CACHE_CONTROL_LONG;
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private static String getContentType(String extension) {
        String mimeType = MIME_TYPES.get(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * 校验 If-None-Match
     */
    private boolean checkIfNoneMatch(HttpRequest request, String etag) {
        String ifNoneMatch = request.getHeader("If-None-Match");
        return ifNoneMatch != null && (ifNoneMatch.equals(etag) || ifNoneMatch.equals("*"));
    }

}
