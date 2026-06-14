package qingzhou.web;

import java.net.URL;
import java.util.*;
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
@Component(property = HttpHandler.HANDLE_PATH + "=")
public class StaticResourceHandler implements HttpHandler {
    @Reference
    private Crypto crypto;

    private Set<String> imageExtensions; // 图片扩展名集合
    private Map<String, String> mimeTypes; // MIME 类型映射
    private final Map<String, WebResource> resources = new ConcurrentHashMap<>(); // 资源缓存

    @Activate
    public void init() {
        imageExtensions = new HashSet<>(Arrays.asList(
                "png", "jpg", "jpeg", "gif", "svg", "ico"));

        mimeTypes = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put("html", "text/html; charset=UTF-8");
            put("htm", "text/html; charset=UTF-8");
            put("js", "application/javascript; charset=UTF-8");
            put("css", "text/css; charset=UTF-8");
            put("json", "application/json; charset=UTF-8");
            put("png", "image/png");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("gif", "image/gif");
            put("svg", "image/svg+xml");
            put("ico", "image/x-icon");
            put("woff", "font/woff");
            put("woff2", "font/woff2");
            put("ttf", "font/ttf");
            put("eot", "application/vnd.ms-fontobject");
            put("otf", "font/otf");
        }});
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
        } else {
            httpResponse.status400Finish();
            return;
        }

        // 安全修复（路径穿越）：getResource 在目录型 ClassLoader 下会解析 ".."，
        // 形如 /web/../../x 的请求可能读取到 webapp/ 之外的资源，故含非法字符的路径直接拒绝
        if (!isPathSafe(requestPath)) {
            httpResponse.status400Finish();
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
            path = "/index.html";
        }

        // 查找资源
        String resourcePath = "/webapp" + path.substring(1);
        WebResource webResource = resources.computeIfAbsent(resourcePath, s -> {
            URL resource = getClass().getResource(s);
            if (resource == null) {
                return null;
            }
            return new WebResource(crypto.getMessageDigest(), crypto.getBase16Coder(),
                    resource);
        });
        if (webResource == null) {
            response.status404Finish();
            return;
        }

        // 扩展名只解析一次，供 Cache-Control 与 Content-Type 共用
        String extension = getExtension(path);
        response.header("Cache-Control", getCacheControl(extension));

        try {
            String etag = webResource.getETag();
            response.header("ETag", etag) // 返回 ETag（MD5）
                    .header("Vary", "Accept-Encoding, Accept"); // 返回辅助缓存头

            // 校验缓存
            boolean isEtagMatch = checkIfNoneMatch(httpRequest, etag);
            if (isEtagMatch) { // 资源未修改 → 直接返回 304
                response.status(304).finish();
            } else { // 响应资源内容
                response.contentType(getContentType(extension))
                        .sendFinish(webResource.getContent());
            }
        } catch (Exception e) {
            response.status500Finish("internal server error");
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

    private String getCacheControl(String extension) {
        if (extension.equals("html") || extension.equals("htm")) {
            return "max-age=0, no-cache, must-revalidate, proxy-revalidate, no-transform";
        }
        if (imageExtensions.contains(extension)) {
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
