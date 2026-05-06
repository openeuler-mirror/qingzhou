package qingzhou.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

/**
 * 静态资源服务，提供前端 UI 的静态文件访问
 */
@Component(property = HttpHandler.HANDLE_PATH + "=/console")
public class StaticResourceHandler implements HttpHandler {

    private static final String STATIC_RESOURCE_PATH = "/webapp/";
    private static final String INDEX_FILE = "index.html";

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
        serveStaticResource(requestPath, httpResponse);
    }

    /**
     * 提供静态资源
     */
    private void serveStaticResource(String path, HttpResponse response) {
        // 规范化路径
        if (path.equals("/") || path.isEmpty()) {
            path = "/" + INDEX_FILE;
        }

        // 移除开头的斜杠
        String resourcePath = STATIC_RESOURCE_PATH + path.substring(1);

        // 尝试从 classpath 加载资源
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            // 如果文件不存在且不是 index.html，尝试返回 index.html（支持前端路由）
            if (!path.endsWith(INDEX_FILE)) {
                serveStaticResource("/" + INDEX_FILE, response);
                return;
            }
            response.status(404);
            response.sendFinish("not found: " + path);
            return;
        }

        try {
            // 设置 Content-Type
            String contentType = getContentType(path);
            response.contentType(contentType);

            // 读取并发送文件内容
            byte[] content = readAllBytes(inputStream);
            response.sendFinish(content);
        } catch (IOException e) {
            response.statusError();
            response.sendFinish("internal server error");
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
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
     * 读取 InputStream 的所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
