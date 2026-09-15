package qingzhou.registry.web;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.InputType;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/invoke")
public class Invoke implements HttpHandler {
    private static final String PATH_PREFIX = "/registry/invoke/"; // HANDLE_PATH=/invoke 加上 qingzhou-registry 的 bundle 前缀

    @Reference
    private Registry registry;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private File uploadBase;

    @Activate
    public void init() {
        uploadBase = Paths.get(System.getProperty("qingzhou.instance"), "temp", "qingzhou-upload").toFile();
        cleanLegacyUploads();
    }

    private void cleanLegacyUploads() {
        File[] legacy = uploadBase.listFiles();
        if (legacy == null) return;
        for (File file : legacy) {
            deleteRecursively(file);
        }
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        RequestImpl request = buildRequest(httpRequest);
        if (request == null) {
            httpResponse.status400Finish();
            return;
        }

        AppStub app = registry.getAppStub(request.getInstance(), request.getApp());

        if (app == null) {
            httpResponse.status400Finish();
            return;
        }

        if (!checkPermission(httpRequest, httpResponse, app, request)) return;

        try {
            parseBodyParameters(httpRequest, request);
            app.invokeApp(request);
        } catch (Throwable e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
            return;
        }

        sendResponse(request, httpResponse);
    }

    private boolean checkPermission(HttpRequest httpRequest, HttpResponse httpResponse, AppStub app, RequestImpl request) {
        try {
            if (isAllowed(httpRequest, app, request)) return true;
            httpResponse.status(403).sendFinish("Forbidden");
        } catch (RuntimeException e) {
            httpResponse.status500Finish("Permission service error");
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean isAllowed(HttpRequest httpRequest, AppStub app, RequestImpl request) {
        Object attribute = httpRequest.getAttribute(AuthResult.AUTH_ROLES_ATTRIBUTE);
        Set<?> roles = attribute instanceof Set ? (Set<?>) attribute : Collections.emptySet();
        App metadata = app.getAppMeta().getApp();
        if (!matchesRoles(roles, metadata.roles)) return false;
        for (Model model : metadata.models) {
            if (!model.code.equals(request.getModel())) continue;
            for (ModelAction action : model.actions) {
                if (action.code.equals(request.getAction())) return matchesRoles(roles, action.roles);
            }
            break;
        }
        return true; // 目标不存在时，沿用原有分派及错误响应。
    }

    private boolean matchesRoles(Set<?> actual, String declaration) {
        if (declaration == null || declaration.trim().isEmpty()) return true;
        Set<String> required = Arrays.stream(declaration.split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).collect(Collectors.toSet());
        if (required.isEmpty()) throw new IllegalArgumentException("Non-empty role declaration contains no roles");
        return !Collections.disjoint(required, actual);
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

        // 添加 URL 里的参数
        httpRequest.getParameters().forEach((k, v) -> request.getParameters().put(k, v.get(0)));
        return request;
    }

    // 添加 POST 请求体里的参数
    private void parseBodyParameters(HttpRequest httpRequest, RequestImpl request) {
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) return;
        if (httpRequest.getContentType() == null) return;
        if (httpRequest.getBody() == null) return;
        if (httpRequest.getBody().length == 0) return;

        // JSON 参数
        if (httpRequest.getContentType().contains("application/json")) {
            try {
                Object parsed = json.fromJson(
                        new String(httpRequest.getBody(), StandardCharsets.UTF_8), Object.class);
                if (parsed instanceof Map) {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) parsed).entrySet()) {
                        request.getParameters().put(
                                String.valueOf(entry.getKey()),
                                entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
                    }
                }
            } catch (Exception e) {
                logger.warn("failed to parse json body: " + e.getMessage());
            }
        }
    }

    private void sendResponse(RequestImpl request, HttpResponse httpResponse) {
        ResponseImpl response = request.getResponse();
        if (!response.isActionInvoked()
                && response.getData() == null
                && response.getMsg() == null) {
            httpResponse.status(404).finish();
            return;
        }

        if (response.getStatus() > 0) {
            httpResponse.status(response.getStatus());
        }
        response.getHeaders().forEach(httpResponse::header);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        if (response.getData() != null) {
            result.put("data", response.getData());
        }
        if (response.getMsg() != null) {
            result.put("msg", response.getMsg());
            result.put("msgLevel", response.getMsgLevel());
        }
        try {
            if (response.getContentType() != null) {
                httpResponse.contentType(response.getContentType());
            } else {
                httpResponse.contentTypeJsonUtf8();
            }
            String json = this.json.toJson(result);
            httpResponse.sendFinish(json);
        } catch (Exception e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public StreamHandler buildStreamHandler() {
        return new StreamHandlerImpl();
    }

    private class StreamHandlerImpl implements StreamHandler {
        HttpRequest httpRequest;
        HttpResponse httpResponse;

        RequestImpl request;
        AppStub app;

        MultipartStreamParser parser;
        boolean terminated;

        @Override
        public void onBegin(HttpRequest httpRequest, HttpResponse httpResponse) {
            if (terminated) return;
            this.httpRequest = httpRequest;
            this.httpResponse = httpResponse;

            request = buildRequest(httpRequest);
            if (request == null) return;

            app = registry.getAppStub(request.getInstance(), request.getApp());
            if (app == null) return;

            terminated = !checkPermission(httpRequest, httpResponse, app, request);
            if (terminated) return;

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
            if (boundary == null) return;

            this.parser = new MultipartStreamParser(
                    boundary, uploadBase,
                    fieldName -> isUploadField(request, fieldName)
            );
        }

        @Override
        public void onNext(byte[] data) {
            if (terminated || parser == null) return;
            try {
                parser.feed(data, false);
            } catch (Exception e) {
                onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (terminated) return;
            terminated = true;
            if (parser == null) {
                httpResponse.status400Finish();
                return;
            }

            try {
                parser.feed(new byte[0], true);
                applyParserResults(parser, request);
                app.invokeApp(request);
            } catch (Throwable e) {
                httpResponse.status500Finish(e.getMessage());
                logger.error(e.getMessage(), e);
                return;
            } finally {
                parser.abort();
            }

            sendResponse(request, httpResponse);
        }

        @Override
        public void onError(Throwable t) {
            if (terminated) return;
            terminated = true;
            if (parser != null) {
                parser.abort();
            }
            if (httpResponse != null) {
                httpResponse.status500Finish(t.getMessage());
            }
            logger.error(t.getMessage(), t);
        }

        boolean isUploadField(RequestImpl request, String fieldName) {
            for (Model model : app.getAppMeta().getApp().models) {
                if (model.code.equals(request.getModel())) {
                    for (ModelField field : model.fields) {
                        if (field.code.equals(fieldName)) {
                            return field.input_type == InputType.file;
                        }
                    }
                }
            }
            return false;
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
                    if (combined.length() > 0) combined.append(",");
                    combined.append(path);
                }
                request.getParameters().put(fieldName, combined.toString());
            }
            request.getUploadFileFields().addAll(parser.getUploadFileFields());
        }
    }
}
