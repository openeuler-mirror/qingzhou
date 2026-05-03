package qingzhou.ai;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ChatModel;
import qingzhou.llm.LLM;
import qingzhou.llm.Listener;
import qingzhou.logger.Logger;
import qingzhou.registry.I18nService;

@Component(property = HttpHandler.HANDLE_PATH + "=/ai/chat",
        configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpHandler implements HttpHandler {
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
    private Collection<qingzhou.llm.Tool> tools;

    @Activate
    public void init(Map<String, String> config) {
        String baseUrl = config.get("base_url");
        String apiKey = config.get("api_key");
        String model = config.get("model");

        chatModel = llm.buildChatModel(baseUrl, apiKey, model);
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
        chatModel.generate(message, tools(), new Listener() {
            final String messageId = UUID.randomUUID().toString().replace("-", "");
            boolean isReasoning = false;
            boolean isMessage = false;

            void sendEvent(String event, String data) {
                httpResponse.send("event: " + event + "\ndata: " + data + "\n\n");
            }

            @Override
            public void onBegin() {
                sendEvent("RUN_STARTED", "{}");
            }

            @Override
            public void onReasoning(String content) {
                if (!isReasoning) {
                    isReasoning = true;
                    if (isMessage) {
                        sendEvent("TEXT_MESSAGE_END", String.format("{\"messageId\":\"%s\"}", messageId));
                    }
                    sendEvent("REASONING_START", "{}");
                }
                sendEvent("REASONING_CONTENT", String.format("{\"content\":\"%s\"}", content));
            }

            @Override
            public void onMessage(String content) {
                if (!isMessage) {
                    isMessage = true;
                    if (isReasoning) {
                        sendEvent("REASONING_END", "{}");
                    }
                    sendEvent("TEXT_MESSAGE_START", String.format("{\"messageId\":\"%s\"}", messageId));
                }

                String res = String.format("{\"messageId\":\"%s\",\"content\":\"%s\"}",
                        messageId,
                        content
                );
                sendEvent("TEXT_MESSAGE_CONTENT", res);
            }

            @Override
            public void onError(Throwable t) {
                String errMsg = t.getMessage();
                logger.error(errMsg, t);

                if (errMsg.length() > 200) errMsg = errMsg.substring(0, 200) + "...";
                sendEventFinish(httpResponse, errMsg);
            }

            @Override
            public void onComplete() {
                sendEvent("TEXT_MESSAGE_END", String.format("{\"messageId\":\"%s\"}", messageId));
                sendEvent("RUN_FINISHED", "{}");
                httpResponse.finish();
            }
        });
    }

    private Collection<qingzhou.llm.Tool> tools() {
        if (tools == null) {
            tools = new HashSet<>();
            
        }
        return tools;
    }

    public void sendEventFinish(HttpResponse writer, String error) {
        String errorJson = "{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + error + "\"}";
        writer.sendFinish("event: RUN_ERROR" + "\ndata: " + errorJson + "\n\n");
    }
}
