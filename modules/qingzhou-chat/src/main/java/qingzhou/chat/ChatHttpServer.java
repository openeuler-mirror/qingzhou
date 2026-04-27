package qingzhou.chat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.agentsflex.core.model.chat.ChatModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=/ai/chat", configurationPid = "qingzhou-chat", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpServer implements HttpHandler {
    @Reference
    private Logger logger;
    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private I18nService i18nService;

    private LlmService llmService;

    @Activate
    public void init(Map<String, String> config) {
        ChatModel chatModel = ModelConfig.getChatModel(config);
        llmService = new LlmService(registry, i18nService, chatModel);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {

        // ========== 1. 设置 SSE 响应头 ==========
        httpResponse.contentType("text/event-stream; charset=utf-8");
        httpResponse.header("Cache-Control", "no-cache");
        httpResponse.header("Connection", "keep-alive");
        httpResponse.header("X-Accel-Buffering", "no");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final SseEmitter sseEmitter = new SseEmitter(httpResponse, countDownLatch);
        // ========== 2. 解析请求体 ==========

        ChatData chatData = parseBody(httpRequest);
        if (chatData == null) {
            sseEmitter.completeWithError(errorJson("请求体为空或格式错误"));
            return;
        }
        String userContent = chatData.getMessage();
        if (userContent == null || userContent.isEmpty()) {
            sseEmitter.completeWithError(errorJson("消息内容不能为空"));
            return;
        }
        if (llmService == null) {
            sseEmitter.completeWithError(errorJson("大模型配置异常，初始化失败"));
            return;
        }
        try {
            chatSteam(userContent, sseEmitter);
        } catch (Throwable e) {
            logger.error("ChatHttpServer exception", e);
            sseEmitter.completeWithError(errorJson("内部服务错误: " + (e.getMessage() != null ? e.getMessage() : "unknown")));
        } finally {
            try {
                countDownLatch.await();
                System.out.println("对话结束！");
            } catch (InterruptedException ignored) {
            }
        }
    }


    private void chatSteam(String userContent, SseEmitter sseEmitter) {
        // ========== 3. 发送 SSE 事件流 ==========
        AtomicReference<SseEventType> reference = new AtomicReference<>();
        reference.set(SseEventType.RUN_STARTED);
        sseEmitter.sendEvent(SseEventType.RUN_STARTED, "{}");

        String messageId = java.util.UUID.randomUUID().toString();
        llmService.streamChat(userContent, new StreamCallback() {

            @Override
            public void onReason(String reason) {
                if (reference.get() != SseEventType.REASONING_CONTENT) {
                    if (reference.get() == SseEventType.TEXT_MESSAGE_CONTENT) {
                        sseEmitter.sendEvent(SseEventType.TEXT_MESSAGE_END, String.format("{\"messageId\":\"%s\"}", messageId));
                    }
                    sseEmitter.sendEvent(SseEventType.REASONING_START, "{}");
                    reference.set(SseEventType.REASONING_CONTENT);
                }
                sseEmitter.sendEvent(SseEventType.REASONING_CONTENT, String.format("{\"content\":%s}", new Gson().toJson(reason)));
            }

            @Override
            public void onToken(String token) {
                if (reference.get() != SseEventType.TEXT_MESSAGE_CONTENT) {
                    if (reference.get() == SseEventType.REASONING_CONTENT) {
                        sseEmitter.sendEvent(SseEventType.REASONING_END, "{}");
                    }
                    sseEmitter.sendEvent(SseEventType.TEXT_MESSAGE_START, String.format("{\"messageId\":\"%s\"}", messageId));
                    reference.set(SseEventType.TEXT_MESSAGE_CONTENT);
                }

                /*
                 * 【安全说明】
                 * token 中可能包含换行符、引号等特殊字符，
                 * 必须正确转义为 JSON 字符串，否则会破坏 SSE data 格式。
                 */
                String res = String.format("{\"messageId\":\"%s\",\"content\":%s}",
                        messageId,
                        new Gson().toJson(token)
                );
                sseEmitter.sendEvent(SseEventType.TEXT_MESSAGE_CONTENT, res);
            }

            @Override
            public void onComplete() {
                reference.set(SseEventType.TEXT_MESSAGE_END);
                // 3.4 文本消息结束
                sseEmitter.sendEvent(SseEventType.TEXT_MESSAGE_END, String.format("{\"messageId\":\"%s\"}", messageId));
            }

            @Override
            public void onFinished() {
                reference.set(SseEventType.RUN_FINISHED);
                // 3.5 运行完成
                sseEmitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                String errMsg = error.getMessage();
                if (errMsg == null) errMsg = "内部服务错误";
                if (errMsg.length() > 200) errMsg = errMsg.substring(0, 200) + "...";
                sseEmitter.completeWithError(errorJson(errMsg));
            }
        });
    }

    /**
     * 构建错误事件 JSON
     */
    private String errorJson(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("code", "INTERNAL_ERROR");
        obj.addProperty("message", message);
        return new Gson().toJson(obj);

    }

    /**
     * 解析请求体 JSON
     */
    private ChatData parseBody(HttpRequest request) {
        try {
            byte[] body = request.getBody();
            return json.fromJson(new String(body, StandardCharsets.UTF_8), ChatData.class);
        } catch (Exception e) {
            System.err.println("chat request body parsing failed: " + e.getMessage());
            return null;
        }
    }
}
