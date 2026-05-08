package qingzhou.ai;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.*;
import qingzhou.api.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ChatModel;
import qingzhou.llm.LLM;
import qingzhou.llm.Listener;
import qingzhou.llm.Tool;
import qingzhou.logger.Logger;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat",
        configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpHandler implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private LLM llm;
    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private I18nService i18nService;

    private final String[] MSG_ERROR = {"消息不存在或数据格式异常", "en:Message not found or data format invalid"};

    private ChatModel chatModel;
    private final Set<Tool> tools = new HashSet<>();

    @Activate
    public void init(Map<String, String> config) {
        String baseUrl = config.get("base_url");
        String apiKey = config.get("api_key");
        String model = config.get("model");

        chatModel = llm.buildChatModel(baseUrl, apiKey, model);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindTool(Tool tool) {
        tools.add(tool);
    }

    public void unbindTool(Tool tool) {
        tools.remove(tool);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // 解析请求
        String message = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                Map<String, String> map = json.fromJson(str, HashMap.class);
                message = map.get("message");
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str);
            }
        }
        if (message == null) {
            String langParam = httpRequest.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
            sendEventFinish(httpResponse, i18nService.getI18n(MSG_ERROR, langParam));
            return;
        }

        // 发出响应
        final String messageId = UUID.randomUUID().toString().replace("-", "");
        httpResponse.contentTypeJsonUtf8();// 返回内容是字符串，非二进制流
        sendEvent(httpResponse, "RUN_STARTED", "{}");
        chatModel.generate(message, tools, new Listener() {
            boolean isReasoning = false;
            boolean isMessage = false;

            @Override
            public void onReasoning(String content) {
                if (!isReasoning) {
                    isReasoning = true;
                    if (isMessage) {
                        sendEvent(httpResponse, "TEXT_MESSAGE_END", toJson(messageId, null));
                    }
                    isMessage = false;
                    sendEvent(httpResponse, "REASONING_START", "{}");
                }
                sendEvent(httpResponse, "REASONING_CONTENT", toJson(null, content));
            }

            @Override
            public void onMessage(String content) {
                if (!isMessage) {
                    isMessage = true;
                    if (isReasoning) {
                        sendEvent(httpResponse, "REASONING_END", "{}");
                    }
                    isReasoning = false;
                    sendEvent(httpResponse, "TEXT_MESSAGE_START", toJson(messageId, null));
                }
                sendEvent(httpResponse, "TEXT_MESSAGE_CONTENT", toJson(messageId, content));
            }

            @Override
            public void onError(Throwable t) {
                String errMsg = t.getMessage();
                logger.error(errMsg);
                sendEventFinish(httpResponse, errMsg);
            }

            @Override
            public void onComplete() {
                sendEvent(httpResponse, "TEXT_MESSAGE_END", String.format("{\"messageId\":\"%s\"}", messageId));
                sendEvent(httpResponse, "RUN_FINISHED", "{}");
                httpResponse.finish();
            }
        });
    }

    private void sendEvent(HttpResponse writer, String event, String data) {
        writer.send("event: " + event + "\ndata: " + data + "\n\n");
    }

    private void sendEventFinish(HttpResponse writer, String error) {
        String errorJson = "{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + error + "\"}";
        writer.sendFinish("event: RUN_ERROR" + "\ndata: " + errorJson + "\n\n");
    }

    private String toJson(String messageId, String content) {
        try {
            Map<String, String> map = new HashMap<>();
            if (messageId != null && !messageId.isEmpty()) {
                map.put("messageId", messageId);
            }
            if (content != null && !content.isEmpty()) {
                map.put("content", content);
            }
            return json.toJson(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
