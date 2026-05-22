package qingzhou.registry.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.InputType;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/invoke")
public class InvokeHttpHandler implements HttpHandler {
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

        try {
            parseRequestBody(httpRequest, request);
            app.invokeApp(request);
        } catch (Throwable e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
            return;
        } finally {
            request.getFileFields().forEach(s -> {
                File tempFile = new File(request.getParameter(s));
                tempFile.delete(); // 上传的文件
                tempFile.getParentFile().delete(); // 随机目录
            });
        }

        ResponseImpl response = request.getResponse();
        if (response.isActionInvoked()
                || response.getData() != null
                || response.getMsg() != null) {
            sendResponse(response, httpResponse);
        } else {
            httpResponse.status404Finish();
        }
    }

    private void sendResponse(ResponseImpl response, HttpResponse httpResponse) {
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
                httpResponse.contentType("application/json; charset=utf-8");
            }
            String json = this.json.toJson(result);
            httpResponse.sendFinish(json);
        } catch (Exception e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    private RequestImpl buildRequest(HttpRequest httpRequest) {
        String requestPath = httpRequest.getPath();
        int i = requestPath.indexOf("/", 1);
        i = requestPath.indexOf("/", i + 1);
        String restPath = requestPath.substring(i + 1);
        String[] rest = restPath.split("/");

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

    private void parseRequestBody(HttpRequest httpRequest, RequestImpl request) {
        // 添加 POST 请求体里的参数
        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && httpRequest.getContentType() != null
                && httpRequest.getBody() != null && httpRequest.getBody().length > 0) {
            if (httpRequest.getContentType().contains("application/x-www-form-urlencoded")) { // 表单参数
                QueryStringDecoder bodyDecoder = new QueryStringDecoder(
                        "/?" + new String(httpRequest.getBody(), StandardCharsets.UTF_8));
                Map<String, List<String>> bodyParams = bodyDecoder.parameters();
                bodyParams.forEach((k, v) -> request.getParameters().put(k, v.get(0)));
            } else if (httpRequest.getContentType().contains("application/json")) { // JSON 参数
                try {
                    Map<String, String> bodyParams = json.fromJson(
                            new String(httpRequest.getBody(), StandardCharsets.UTF_8), Map.class);
                    bodyParams.forEach((k, v) -> request.getParameters().put(k, v));
                } catch (Exception e) {
                    logger.warn("failed to parse json body: " + e.getMessage());
                }
            } else if (httpRequest.getContentType().contains("multipart/form-data")) { // 文件上传参数
                parseMultipart(httpRequest, request);
            }
        }
    }

    private void parseMultipart(HttpRequest httpRequest, RequestImpl request) {
        String boundary = extractBoundary(httpRequest.getContentType());
        if (boundary == null) {
            logger.warn("missing boundary in multipart content type");
            return;
        }

        try {
            byte[] body = httpRequest.getBody();
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String[] parts = bodyStr.split("--" + boundary);

            for (String part : parts) {
                if (part.trim().isEmpty() || part.contains("--"))
                    continue;

                int headerEnd = part.indexOf("\r\n\r\n");
                if (headerEnd == -1)
                    continue;

                String headers = part.substring(0, headerEnd);
                String content = part.substring(headerEnd + 4);

                // 提取文件名和字段名
                String fieldName = extractFieldName(headers);
                String fileName = extractFileName(headers);

                if (fieldName != null) {
                    if (fileName != null && !fileName.isEmpty()
                            && !fileName.contains("..")
                            && !fileName.contains("/")
                            && !fileName.contains("\\")) {
                        boolean isUpload = false;
                        AppStub appStub = registry.getAppStub(request.getInstance(), request.getApp());
                        if (appStub != null) {
                            App app = appStub.getAppMeta().getApp();
                            for (Model model : app.models) {
                                if (model.code.equals(request.getModel())) {
                                    for (ModelField field : model.fields) {
                                        if (field.code.equals(fieldName)) {
                                            isUpload = field.input_type == InputType.file;
                                        }
                                    }
                                }
                            }
                        }
                        if (isUpload) {
                            String uploadId = UUID.randomUUID().toString();
                            File uploadDir = new File(uploadBase, uploadId);
                            uploadDir.mkdirs();
                            File tempFile = new File(uploadDir, fileName);
                            Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
                            request.getParameters().put(fieldName, tempFile.getAbsolutePath());
                            request.getFileFields().add(fieldName);
                        } else {
                            request.getParameters().put(fieldName, fileName);
                        }
                    } else {
                        // 普通字段
                        request.getParameters().put(fieldName, content.trim());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("failed to parse multipart request", e);
        }
    }

    private String extractBoundary(String contentType) {
        if (contentType == null)
            return null;
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring(9).replace("\"", "");
            }
        }
        return null;
    }

    private String extractFieldName(String headers) {
        int start = headers.indexOf("name=\"");
        if (start == -1)
            return null;
        start += 6;
        int end = headers.indexOf("\"", start);
        if (end == -1)
            return null;
        return headers.substring(start, end);
    }

    private String extractFileName(String headers) {
        int start = headers.indexOf("filename=\"");
        if (start == -1)
            return null;
        start += 10;
        int end = headers.indexOf("\"", start);
        if (end == -1)
            return null;
        return headers.substring(start, end);
    }
}
