package qingzhou.registry.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.QueryStringDecoder;
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
            }
        }

        return request;
    }
}
