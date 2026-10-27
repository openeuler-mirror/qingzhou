package qingzhou.registry.web;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.InputType;
import qingzhou.common.TempFiles;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

@Component(configurationPid = "qingzhou-registry", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=/invoke", HttpHandler.HANDLE_METHODS + "=POST"})
public class Invoke implements HttpHandler {
    public static final String UPLOAD_TEMP_NAME = "qingzhou-upload";
    private static final String PATH_PREFIX = "/registry/invoke/"; // HANDLE_PATH=/invoke 加上 qingzhou-registry 的 bundle 前缀
    // 运行期惰性清理：上传中断残留的临时文件超过该时限即删除，防止反复失败积压占盘（agent 侧同策略）
    private static final long TEMP_FILE_TTL_MILLIS = 24L * 60 * 60 * 1000;
    // 参数表上限：单个请求体可解析出百万级小参数（每条目数十字节开销），须限数量与单值长度
    private static final int MAX_PARAMETERS = 1000;
    private static final int MAX_PARAMETER_CHARS = 1024 * 1024;

    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private File uploadBase;

    @Activate
    public void init(Map<String, String> config) {
        uploadBase = Paths.get(config.get("qingzhou.instance"), "temp", UPLOAD_TEMP_NAME).toFile();
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS); // 只清过期残留：配置刷新重激活不得删除在途上传
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        TempFiles.cleanExpired(uploadBase, TEMP_FILE_TTL_MILLIS);

        Prepared prepared = prepare(httpRequest, httpResponse);
        if (prepared == null) return;

        try {
            if (!parseBodyParameters(httpRequest, prepared.request)) {
                httpResponse.status(400); // 参数规模超限：明确拒绝，不让参数表把内存撑爆
                return;
            }
            prepared.app.invokeApp(prepared.request);
        } catch (Throwable e) {
            logger.error("http invoke failed", e);
            httpResponse.status(500);
            return;
        }

        sendResponse(prepared.request, httpResponse);
    }

    // 聚合与流式共用的请求准备：校验路由/应用/权限；错误分支须提前收尾，流式链路不能等到 onComplete
    private Prepared prepare(HttpRequest httpRequest, HttpResponse httpResponse) {
        RequestImpl request = buildRequest(httpRequest);
        if (request == null) {
            httpResponse.status(400).finish();
            return null;
        }

        AppStub app = registry.getAppStub(request.getInstance(), request.getApp());
        if (app == null) {
            httpResponse.status(400).finish();
            return null;
        }

        return permissionForbidReq(httpRequest, httpResponse, app, request) ? null : new Prepared(request, app);
    }

    private static final class Prepared {
        final RequestImpl request;
        final AppStub app;

        Prepared(RequestImpl request, AppStub app) {
            this.request = request;
            this.app = app;
        }
    }

    private boolean permissionForbidReq(HttpRequest httpRequest, HttpResponse httpResponse, AppStub appStub, RequestImpl request) {
        String[] roles = httpRequest.getRoles();
        if (PermissionChecker.isAllowed(appStub.getAppMeta().getApp(), request.getModel(), request.getAction(), roles)) {
            return false;
        } else {
            logger.warn("forbidden: app=" + forLog(request.getApp()) + ", model=" + forLog(request.getModel())
                    + ", action=" + forLog(request.getAction()) + ", user=" + forLog(httpRequest.getPrincipal())
                    + ", host=" + forLog(httpRequest.getRemoteHost()));
            httpResponse.status(403).send("Forbidden").finish();
            return true;
        }
    }

    private RequestImpl buildRequest(HttpRequest httpRequest) {
        String requestPath = httpRequest.getPath();
        if (!requestPath.startsWith(PATH_PREFIX)) return null;

        // 过滤空段：形如 // 的双斜杠不应产生空的 instance/app 段
        String[] rest = Arrays.stream(requestPath.substring(PATH_PREFIX.length()).split("/"))
                .filter(segment -> !segment.isEmpty()).toArray(String[]::new);

        int restMinDepth = 4; // instance/app/model/action/[id]
        if (rest.length < restMinDepth)
            return null;

        RequestImpl request = new RequestImpl();
        request.setInstance(rest[0]);
        request.setApp(rest[1]);
        request.setModel(rest[2]);
        request.setAction(rest[3]);
        if (rest.length > 4) {
            request.setId(rest[4]);
        }

        // 添加 URL 里的参数：超长参数明确拒绝，静默丢弃会让业务在缺参状态下继续执行
        for (Map.Entry<String, List<String>> entry : httpRequest.getParameters().entrySet()) {
            List<String> values = entry.getValue(); // 畸形参数表可能产生空值列表
            if (values == null || values.isEmpty()) continue;
            String value = values.get(0);
            if (value.length() > MAX_PARAMETER_CHARS) {
                logger.warn("invoke parameter too long, rejected: " + forLog(entry.getKey()));
                return null;
            }
            request.getParameters().put(entry.getKey(), value);
        }
        return request;
    }

    // 添加 POST 请求体里的参数；返回 false 表示参数规模超出上限
    private boolean parseBodyParameters(HttpRequest httpRequest, RequestImpl request) {
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) return true;
        if (httpRequest.getContentType() == null) return true;
        if (httpRequest.getBody() == null) return true;
        if (httpRequest.getBody().length == 0) return true;

        // JSON 参数
        if (httpRequest.getContentType().contains("application/json")) {
            try {
                Object parsed = json.fromJson(
                        new String(httpRequest.getBody(), StandardCharsets.UTF_8), Object.class);
                if (parsed instanceof Map) {
                    Map<String, String> parameters = request.getParameters();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) parsed).entrySet()) {
                        String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                        if (parameters.size() >= MAX_PARAMETERS || value.length() > MAX_PARAMETER_CHARS) {
                            logger.warn("invoke parameters exceed quota, rejected: instance=" + forLog(request.getInstance())
                                    + ", app=" + forLog(request.getApp()));
                            return false;
                        }
                        parameters.put(String.valueOf(entry.getKey()), value);
                    }
                }
            } catch (Exception e) {
                // 体被声明为 JSON 却解析失败：不得用残缺参数继续调用业务
                logger.warn("failed to parse json body: " + e.getClass().getSimpleName()
                        + ", body length=" + httpRequest.getBody().length);
                return false;
            }
        }
        return true;
    }

    private void sendResponse(RequestImpl request, HttpResponse httpResponse) {
        ResponseImpl response = request.getResponse();
        if (!response.isActionInvoked()
                && response.getData() == null
                && response.getMsg() == null) {
            httpResponse.status(404);
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        if (response.getData() != null) {
            result.put("data", response.getData());
        }
        if (response.getMsg() != null) {
            result.put("msg", response.getMsg());
            result.put("msgLevel", response.getMsgLevel());
        }
        // 状态码与响应头来自远端 agent：非法值在 HttpResponse 侧抛 IAE，须与序列化失败同分支收尾
        try {
            if (response.getStatus() > 0) {
                httpResponse.status(response.getStatus());
            }
            response.getHeaders().forEach((name, value) -> {
                if (isPassthroughHeader(name)) httpResponse.header(name, value);
            });

            if (response.getContentType() != null) {
                httpResponse.contentType(response.getContentType());
            } else {
                httpResponse.contentTypeJsonUtf8();
            }
            String json = this.json.toJson(result);
            httpResponse.send(json);
        } catch (Exception e) {
            logger.error("failed to serialize invoke response", e);
            httpResponse.status(500);
        }
    }

    @Override
    public StreamHandler multipartStreamHandler() {
        return new StreamHandlerImpl();
    }

    // RFC 2046：boundary 为 1~70 个可打印字符，不含空白、引号与反斜杠
    private static boolean isValidBoundary(String boundary) {
        if (boundary == null || boundary.isEmpty() || boundary.length() > 70) return false;
        for (int i = 0; i < boundary.length(); i++) {
            char c = boundary.charAt(i);
            if (c <= 0x20 || c > 0x7E || c == '"' || c == '\\') return false;
        }
        return true;
    }

    // 请求来源字段不可信：URL 段与参数名可经 %0A 携带换行，入日志前剔除控制字符
    private static String forLog(String value) {
        return value == null ? "null" : value.replaceAll("\\p{Cntrl}", "?");
    }

    // 远端响应头透传前须剔除：长度类与 hop-by-hop 头会破坏中心侧 HTTP 响应分帧；content-type 由下方显式设置；
    // set-cookie 属中心域的会话凭据，不得由远端实例写入
    private static final Set<String> BLOCKED_PASSTHROUGH_HEADERS = new HashSet<>(Arrays.asList(
            "content-length", "transfer-encoding", "connection", "keep-alive", "content-type",
            "set-cookie", "set-cookie2"));

    private static boolean isPassthroughHeader(String name) {
        return name != null && !BLOCKED_PASSTHROUGH_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private class StreamHandlerImpl implements StreamHandler {
        HttpResponse httpResponse;

        RequestImpl request;
        AppStub app;

        MultipartStreamParser parser;
        Set<String> fileFields;

        @Override
        public void onBegin(HttpRequest httpRequest, HttpResponse httpResponse) {
            this.httpResponse = httpResponse;

            Prepared prepared = prepare(httpRequest, httpResponse);
            if (prepared == null) return;

            request = prepared.request;
            app = prepared.app;

            String boundary = null;
            String contentType = httpRequest.getContentType();
            if (contentType != null) {
                for (String part : contentType.split(";")) {
                    part = part.trim();
                    if (part.startsWith("boundary=")) {
                        boundary = part.substring(9).replace("\"", "");
                    }
                }
            }
            if (!isValidBoundary(boundary)) { // 空/超长/含空白与控制字符的分隔符会破坏解析
                httpResponse.status(400).finish();
                return;
            }

            this.parser = new MultipartStreamParser(
                    boundary, uploadBase,
                    fieldName -> isUploadField(request, fieldName)
            );
        }

        @Override
        public void onNext(byte[] data) throws Throwable {
            if (parser != null) {
                parser.feed(data, false);
            }
        }

        @Override
        public void onComplete() {
            if (parser == null) return; // onBegin 已对无效请求写过 400/403，不重复响应

            try {
                parser.feed(new byte[0], true);
                applyParserResults(parser, request);
                app.invokeApp(request);
            } catch (Throwable e) {
                logger.error("multipart invoke failed", e);
                httpResponse.status(500);
                return;
            } finally {
                parser.abort();
            }

            sendResponse(request, httpResponse);
        }

        @Override
        public void onError(Throwable t) {
            if (parser != null) {
                parser.abort();
            }
        }

        // 文件字段集按请求算一次：分片解析会逐片回调，不能每片都全量遍历模型字段
        boolean isUploadField(RequestImpl request, String fieldName) {
            if (fileFields == null) {
                fileFields = new HashSet<>();
                for (Model model : app.getAppMeta().getApp().models) {
                    if (!model.code.equals(request.getModel())) continue;
                    for (ModelField field : model.fields) {
                        if (field.input_type == InputType.file) fileFields.add(field.code);
                    }
                }
            }
            return fileFields.contains(fieldName);
        }

        void applyParserResults(MultipartStreamParser parser, RequestImpl request) {
            request.getParameters().putAll(parser.getParameters());
            Map<String, List<String>> uploadFileMap = parser.getUploadFileMap();
            for (Map.Entry<String, List<String>> entry : uploadFileMap.entrySet()) {
                String fieldName = entry.getKey();
                List<String> paths = entry.getValue();
                String existingText = request.getParameter(fieldName);
                StringBuilder combined = new StringBuilder();
                if (existingText != null && !existingText.isEmpty()) {
                    combined.append(existingText.trim());
                }
                for (String path : paths) {
                    if (combined.length() > 0) combined.append(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP);
                    combined.append(path);
                }
                request.getParameters().put(fieldName, combined.toString());
            }
            request.getUploadFileFields().addAll(parser.getUploadFileFields());
        }
    }
}
