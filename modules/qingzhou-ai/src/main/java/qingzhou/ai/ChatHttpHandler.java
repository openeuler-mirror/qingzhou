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
            String error = i18nService.getI18n(MSG_ERROR, langParam);
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message(error)));
            return;
        }

        // 发出响应
        final String messageId = UUID.randomUUID().toString().replace("-", "");
        httpResponse.contentTypeJsonUtf8();// 返回内容是字符串，非二进制流
        httpResponse.send(resultToString(SseResult.type("RUN_STARTED")));
        chatModel.generate(message, tools, new Listener() {
            boolean isReasoning = false;
            boolean isMessage = false;

            @Override
            public void onReasoning(String content) {
                if (!isReasoning) {
                    isReasoning = true;
                    if (isMessage) {
                        httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
                    }
                    isMessage = false;
                    httpResponse.send(resultToString(SseResult.type("REASONING_START")));
                }
                httpResponse.send(resultToString(SseResult.type("REASONING_CONTENT").content(content)));
            }

            @Override
            public void onMessage(String content) {
                if (!isMessage) {
                    isMessage = true;
                    if (isReasoning) {
                        httpResponse.send(resultToString(SseResult.type("REASONING_END")));
                    }
                    isReasoning = false;
                    httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_START").messageId(messageId)));
                }
                httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_CONTENT").messageId(messageId).content(content)));
            }

            @Override
            public void onError(Throwable t) {
                String errMsg = t.getMessage();
                logger.error(errMsg);
                httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message(errMsg)));
            }

            @Override
            public void onComplete() {
                httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
                httpResponse.sendFinish(resultToString(SseResult.type("RUN_FINISHED")));
            }
        });
    }

    private String resultToString(SseResult result) {
        String toJson;
        try {
            toJson = json.toJson(result.data);
        } catch (Exception e) {
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", result.type, toJson);
    }
}
