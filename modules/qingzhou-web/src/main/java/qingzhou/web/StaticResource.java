package qingzhou.web;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

/**
 * 静态资源服务，提供前端 UI 的静态文件访问
 */
@Component(property = {HttpHandler.HANDLE_PATH + "=/", HttpHandler.HANDLE_NO_AUTH + "=true"})
public class StaticResource implements HttpHandler {
    private static final String INDEX_FILE = "/index.html";

    @Reference
    private Crypto crypto;

    private Set<String> imageExtensions; // 图片扩展名集合
    private Map<String, String> mimeTypes; // MIME 类型映射
    private final Map<String, ResourceCache> resourceCacheMap = new ConcurrentHashMap<>(); // 资源缓存

    @Activate
    public void init() {
        imageExtensions = new HashSet<>(Arrays.asList(
                "png", "jpg", "jpeg", "gif", "svg", "ico"));

        Map<String, String> mimeMap = new HashMap<>();
        mimeMap.put("html", "text/html; charset=UTF-8");
        mimeMap.put("htm", "text/html; charset=UTF-8");
        mimeMap.put("js", "application/javascript; charset=UTF-8");
        mimeMap.put("css", "text/css; charset=UTF-8");
        mimeMap.put("json", "application/json; charset=UTF-8");
        mimeMap.put("png", "image/png");
        mimeMap.put("jpg", "image/jpeg");
        mimeMap.put("jpeg", "image/jpeg");
        mimeMap.put("gif", "image/gif");
        mimeMap.put("svg", "image/svg+xml");
        mimeMap.put("ico", "image/x-icon");
        mimeMap.put("woff", "font/woff");
        mimeMap.put("woff2", "font/woff2");
        mimeMap.put("ttf", "font/ttf");
        mimeMap.put("eot", "application/vnd.ms-fontobject");
        mimeMap.put("otf", "font/otf");
        mimeTypes = Collections.unmodifiableMap(mimeMap);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        String requestPath = httpRequest.getPath();

        // 解码百分号编码（如 %2e%2e、%2f），否则 isPathSafe 会被编码形式的穿越序列绕过
        try {
            requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            httpResponse.status(400).finish();
            return;
        }

        // 修复：仅当路径恰好是 "/web" 或以 "/web/" 开头时才剥离前缀，
        // 避免把 "/website" 等以 "web" 开头的合法路径误当作带 "/web" 前缀而错误截断
        if (requestPath.equals("/web")) {
            requestPath = "/";
        } else if (requestPath.startsWith("/web/")) {
            requestPath = requestPath.substring("/web".length());
        } else {
            httpResponse.status(400).finish();
            return;
        }

        // 安全修复（路径穿越）：getResource 在目录型 ClassLoader 下会解析 ".."，
        // 形如 /web/../../x 的请求可能读取到 webapp/ 之外的资源，故含非法字符的路径直接拒绝
        if (!isPathSafe(requestPath)) {
            httpResponse.status(400).finish();
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
            path = INDEX_FILE;
        }

        // 扩展名只解析一次，供 SPA 回退、Cache-Control 与 Content-Type 共用
        String extension = getExtension(path);

        // 查找资源
        String resourcePath = "/webapp" + path;
        ResourceCache resourceCache = resourceCacheMap.computeIfAbsent(resourcePath, s -> {
            URL resource = getClass().getResource(s);
            if (resource == null) {
                return null;
            }
            return new ResourceCache(crypto.getMessageDigest(), crypto.getBase16Coder(),
                    resource);
        });
        if (resourceCache == null) {
            // SPA 应用特性：无扩展名的路径视为前端路由（如 /web/model/xxx），回退到 index.html；
            // 带扩展名的资源（如已被清理的旧 hash .js/.css）必须返回 404，
            // 否则浏览器会把 HTML 当 JS 解析导致模块加载失败
            if (!path.equals(INDEX_FILE) && extension.isEmpty()) {
                serveStaticResource(httpRequest, INDEX_FILE, response);
                return;
            }
            response.status(404).finish();
            return;
        }

        response.header("Cache-Control", getCacheControl(extension, path));

        try {
            String etag = resourceCache.getETag();
            response.header("ETag", etag) // 返回 ETag（MD5）
                    .header("Vary", "Accept-Encoding, Accept"); // 返回辅助缓存头

            // 校验缓存
            boolean isEtagMatch = checkIfNoneMatch(httpRequest, etag);
            if (isEtagMatch) { // 资源未修改 → 直接返回 304
                response.status(304).finish();
            } else { // 响应资源内容
                response.contentType(getContentType(extension))
                        .send(resourceCache.getContent()).finish();
            }
        } catch (Exception e) {
            response.status(500).finish();
        }
    }

    /**
     * 安全校验：拒绝包含路径穿越段（..）、反斜杠或空字节的路径。
     * 按 "/" 分段比较，避免误伤文件名中合法的连续点（如 a..b.js）
     */
    private boolean isPathSafe(String path) {
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
    private String getExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        // 点必须位于最后一段文件名内，且不是文件名首字符（排除隐藏文件如 .gitignore）
        if (lastDot > lastSlash + 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private String getCacheControl(String extension, String path) {
        if (path.equals(INDEX_FILE)) {
            // 入口 HTML 不长期缓存：发版后老用户若长期缓存 index.html 会一直白屏
            return "max-age=0, must-revalidate";
        }
        if (extension.equals("html") || extension.equals("htm")
                || imageExtensions.contains(extension)) {
            return "max-age=2592000, must-revalidate, proxy-revalidate, no-transform"; // 一个月
        }
        return "max-age=31536000, must-revalidate, proxy-revalidate, no-transform"; // 一年
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(String extension) {
        String mimeType = mimeTypes.get(extension);
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
