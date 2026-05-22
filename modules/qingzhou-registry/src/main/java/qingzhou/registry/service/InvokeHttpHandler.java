package qingzhou.registry.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
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

        // 保存原始文件路径，用于 finally 块中的清理（应用可能修改参数值）
        Map<String, String> originalFilePaths = new HashMap<>();

        try {
            parseRequestBody(httpRequest, request);

            // 在应用处理前保存文件路径，因为应用可能会修改参数值（如将路径改为文件名）
            for (String fileField : request.getFileFields()) {
                originalFilePaths.put(fileField, request.getParameter(fileField));
            }

            app.invokeApp(request);
        } catch (Throwable e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
            return;
        } finally {
            // 使用原始路径清理上传的临时文件
            for (String pathsValue : originalFilePaths.values()) {
                if (pathsValue == null) continue;
                String[] paths = pathsValue.split(",");
                for (String path : paths) {
                    File tempFile = new File(path.trim());
                    if (tempFile.exists()) {
                        tempFile.delete(); // 上传的文件
                    }
                    File parentDir = tempFile.getParentFile();
                    if (parentDir != null && parentDir.isDirectory()) {
                        parentDir.delete(); // 随机目录
                    }
                }
            }
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
            byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
            byte[] headerDelimiter = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

            // 收集同一字段名的多个文件
            Map<String, List<String>> uploadFileMap = new LinkedHashMap<>();

            int pos = 0;
            while (pos < body.length) {
                // 查找下一个 boundary
                int boundaryStart = indexOfBytes(body, boundaryBytes, pos);
                if (boundaryStart == -1) break;

                // 跳过 boundary 标记
                int afterBoundary = boundaryStart + boundaryBytes.length;

                // 检查是否为结束 boundary（--结尾）
                if (afterBoundary + 1 < body.length && body[afterBoundary] == '-' && body[afterBoundary + 1] == '-') {
                    break;
                }

                // 跳过 boundary 后的 CRLF
                if (afterBoundary + 1 < body.length && body[afterBoundary] == '\r' && body[afterBoundary + 1] == '\n') {
                    afterBoundary += 2;
                }

                // 查找头部结束位置（双CRLF）
                int headerEnd = indexOfBytes(body, headerDelimiter, afterBoundary);
                if (headerEnd == -1) break;

                // 解析头部为字符串
                String headers = new String(body, afterBoundary, headerEnd - afterBoundary, StandardCharsets.UTF_8);
                int contentStart = headerEnd + headerDelimiter.length;

                // 查找下一个 boundary 以确定内容结束位置
                int nextBoundary = indexOfBytes(body, boundaryBytes, contentStart);
                if (nextBoundary == -1) break;

                // 内容在下一个 boundary 前的 CRLF 之前结束
                int contentEnd = nextBoundary;
                if (contentEnd >= 2 && body[contentEnd - 2] == '\r' && body[contentEnd - 1] == '\n') {
                    contentEnd -= 2;
                }

                // 提取字段名和文件名
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

                            // 直接写入原始字节，不做字符集转换，确保二进制文件不损坏
                            byte[] contentBytes = new byte[contentEnd - contentStart];
                            System.arraycopy(body, contentStart, contentBytes, 0, contentBytes.length);
                            Files.write(tempFile.toPath(), contentBytes);

                            // 收集同一字段的多个文件路径
                            uploadFileMap.computeIfAbsent(fieldName, k -> new ArrayList<>())
                                    .add(tempFile.getAbsolutePath());
                            request.getFileFields().add(fieldName);
                        } else {
                            request.getParameters().put(fieldName, fileName);
                        }
                    } else {
                        // 普通字段：内容转为字符串
                        String content = new String(body, contentStart, contentEnd - contentStart, StandardCharsets.UTF_8).trim();
                        request.getParameters().put(fieldName, content);
                    }
                }

                pos = nextBoundary;
            }

            // 处理多文件字段：合并已有文本值（如已有文件名）与新文件路径
            for (Map.Entry<String, List<String>> entry : uploadFileMap.entrySet()) {
                String fieldName = entry.getKey();
                List<String> paths = entry.getValue();
                
                // 获取该字段可能已有的文本值（来自 multipart 文本部分）
                String existingText = request.getParameter(fieldName);
                
                // 合并：已有文本 + 新文件路径
                StringBuilder combined = new StringBuilder();
                if (existingText != null && !existingText.isEmpty()) {
                    combined.append(existingText.trim());
                }
                for (String path : paths) {
                    if (combined.length() > 0) {
                        combined.append(",");
                    }
                    combined.append(path);
                }
                request.getParameters().put(fieldName, combined.toString());
            }
        } catch (IOException e) {
            logger.error("failed to parse multipart request", e);
        }
    }

    /**
     * 在字节数组中查找模式字节序列的位置
     */
    private int indexOfBytes(byte[] data, byte[] pattern, int startPos) {
        if (pattern.length == 0) return startPos;
        if (startPos < 0) startPos = 0;
        for (int i = startPos; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
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
