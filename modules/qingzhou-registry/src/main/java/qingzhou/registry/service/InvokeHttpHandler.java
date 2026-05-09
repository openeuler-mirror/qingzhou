package qingzhou.registry.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
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
            app.invokeApp(request);
        } catch (Throwable e) {
            httpResponse.status500Finish(e.getMessage());
            logger.error(e.getMessage(), e);
            return;
        }

        ResponseImpl response = request.getResponse();
        if (response.isActionInvoked()
                || response.getData() != null
                || response.getMsg() != null
        ) {
            httpResponse.contentTypeJsonUtf8();
            sendResponse(response, httpResponse);
        } else {
            httpResponse.status404Finish();
        }
    }

    private void sendResponse(ResponseImpl response, HttpResponse httpResponse) {
        if (response.getStatus() > 0) {
            httpResponse.status(response.getStatus());
        }
        if (response.getContentType() != null) {
            httpResponse.contentType(response.getContentType());
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
        String restPath = requestPath.substring(i + 1);
        String[] rest = restPath.split("/");

        int restMinDepth = 4; // instance/app/model/action/[id]
        if (rest.length < restMinDepth) return null;

        RequestImpl request = new RequestImpl();
        request.setInstance(rest[0]);
        request.setApp(rest[1]);
        request.setModel(rest[2]);
        request.setAction(rest[3]);
        if (rest.length > 4) {
            request.setId(rest[4]);
        }

        // 获取 URL 参数
        httpRequest.getParameters().forEach((k, v) -> request.getParameters().put(k, v.get(0)));

        // 如果是 POST 请求且 Content-Type 为 application/json，需要从请求体中解析参数
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())
                && httpRequest.getContentType() != null
                && httpRequest.getContentType().contains("application/json")) {
            byte[] body = httpRequest.getBody();
            if (body != null && body.length > 0) {
                try {
                    Map<String, Object> bodyParams = json.fromJson(new String(body, StandardCharsets.UTF_8), Map.class);
                    if (bodyParams != null) {
                        bodyParams.forEach((k, v) -> {
                            if (v != null) {
                                request.getParameters().put(k, v.toString());
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.warn("failed to parse JSON body: " + e.getMessage());
                }
            }
        }

        return request;
    }
}
