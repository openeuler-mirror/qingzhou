package qingzhou.ai;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Chat;
import qingzhou.llm.LLM;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/ai/chat",
        configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpServer implements HttpHandler {
    @Reference
    private LLM llm;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private Chat chat;

    @Activate
    public void init(Map<String, String> config) {
        String baseUrl = config.get("base_url");
        String apiKey = config.get("api_key");
        String model = config.get("model");

        chat = llm.buildChat(baseUrl, apiKey, model);
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
        if (message == null || message.trim().isEmpty()) {
            completeWithError(httpResponse, errorJson("消息内容不能为空"));
            return;
        }

        // 发出响应
        String chatted = chat.chat(message.trim());
        httpResponse.sendResponse(chatted);
//        httpResponse.contentType("text/event-stream; charset=utf-8");
    }

//    public void streamChat(String message, StreamCallback callback) {
//        MemoryPrompt prompt = new MemoryPrompt();
//        prompt.addMessage(new UserMessage(message));
//        if (tools == null) {
//            tools = getTools();
//        }
//        prompt.addTools(tools);
//        chatStream(prompt, new StreamResponseListener() {
//
//            @Override
//            public void onMessage(StreamContext context, AiMessageResponse response) {
//                String reasoningContent = response.getMessage().getReasoningContent();
//                if (StringUtil.hasText(reasoningContent)) {
//                    callback.onReason(reasoningContent);
//                }
//                String content = response.getMessage().getContent();
//                if (content != null) {
//                    callback.onToken(content);
//                }
//
//                if (response.getMessage().isFinalDelta() && response.getMessage().getToolCalls() != null) {
//                    prompt.addMessage(response.getMessage());
//                    for (ToolCall toolCall : response.getMessage().getToolCalls()) {
//                        System.out.println(">>>>> " + toolCall.getName() + ": " + JSON.toJSONString(toolCall.getArgsMap(), JSONWriter.Feature.PrettyFormat));
//                    }
//                    List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
//                    prompt.addMessages(toolMessages);
//
//                    chatStream(prompt, this);
//                } else if (response.getMessage().isFinalDelta() && !response.getMessage().hasToolCalls()) {
//                    // 保存完整回复到记忆
//                    prompt.addMessage(response.getMessage());
//                    callback.onComplete();
//                    callback.onFinished();
//                }
//            }
//
//            @Override
//            public void onFailure(StreamContext context, Throwable throwable) {
//                callback.onError(throwable);
//            }
//        });
//    }

    private static final String[] allowAction = new String[]{
            Show.ACTION_CODE_SHOW,
            qingzhou.api.type.List.ACTION_CODE_LIST,
            Monitor.ACTION_CODE_MONITOR
    };

//    private List<Tool> getTools() {
//        List<Tool> tools = new ArrayList<>();
//        List<String> allLocalApps = registry.getAllLocalApps();
//        for (String appCode : allLocalApps) {
//            AppStubLocal appStubLocal = registry.getLocalApp(appCode);
//            App app = appStubLocal.getAppMeta().getApp();
//            for (Model model : app.models) {
//                for (ModelAction action : model.actions) {
//                    String methodName = action.code;
//                    if (!ArrayUtil.contains(allowAction, methodName)) {
//                        continue;
//                    }
//                    String code = model.code;
//                    String description = "[GROUP:" + i18nService.getI18n(app.name, Lang.zh) + "]" + i18nService.getI18n(model.name, Lang.zh) + "模块" + i18nService.getI18n(action.name, Lang.zh) + "方法, " + i18nService.getI18n(action.info, Lang.zh);
//                    Tool.Builder builder = Tool.builder()
//                            .name(appCode + "." + methodName + "_" + code)
//                            .description(description);
//                    ModelField idField = getIdField(model);
//                    if (Show.ACTION_CODE_SHOW.equals(methodName) && idField != null) {
//                        builder.addParameter(Parameter.builder()
//                                .name(idField.fieldName)
//                                .description(i18nService.getI18n(idField.name, Lang.zh))
//                                .type(idField.input_type == InputType.number ? "number" : "string")
//                                .required(true)
//                                .build());
//                    }
//                    if (qingzhou.api.type.List.ACTION_CODE_LIST.equals(methodName)) {
//                        List<ModelField> searchFields = getSearchField(model);
//                        for (ModelField field : searchFields) {
//                            builder.addParameter(Parameter.builder()
//                                    .name(field.fieldName)
//                                    .description(i18nService.getI18n(field.name, Lang.zh))
//                                    .type(field.input_type == InputType.number ? "number" : "string")
//                                    .build());
//                        }
//                    }
//                    Tool tool = builder
//                            .function(args -> {
//                                try {
//                                    RequestImpl request = new RequestImpl();
//                                    request.setApp(appCode);
//                                    request.setModel(code);
//                                    request.setAction(methodName);
//                                    request.setInstance(Constants.LOCAL_INSTANCE_ID);
//                                    if (idField != null) {
//                                        Object o = args.get(idField.fieldName);
//                                        if (o != null) {
//                                            request.setId(o.toString());
//                                        }
//                                    }
//                                    args.forEach((s, o) -> request.getParameters().put(s, o.toString()));
//                                    appStubLocal.invokeApp(request);
//                                    return request.getResponse().getData();
//                                } catch (Throwable e) {
//                                    return "执行异常：" + e.getMessage();
//                                }
//                            })
//                            .build();
//                    tools.add(tool);
//                }
//            }
//        }
//        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(tools));
//        return tools;
//    }

    private ModelField getIdField(Model model) {
        for (ModelField field : model.fields) {
            if (field.id) {
                return field;
            }
        }
        return null;
    }

    private List<ModelField> getSearchField(Model model) {
        List<ModelField> list = new ArrayList<>();
        for (ModelField field : model.fields) {
            if (field.search) {
                list.add(field);
            }
        }
        return list;
    }


    public boolean completeWithError(HttpResponse writer, String error) {
        return sendEvent(writer, SseEventType.RUN_ERROR, error);
    }

    /**
     * 发送一个 SSE 事件
     *
     * @param event 事件类型（event: 行）
     * @param data  事件数据（data: 行），必须是合法 JSON 字符串
     * @return true 发送成功，false 客户端已断开
     */
    public boolean sendEvent(HttpResponse writer, SseEventType event, String data) {
        writer.sendResponse("event: " + event.name() + "\n");
        writer.sendResponse("data: " + data + "\n");
        writer.sendResponse("\n");  // 空行表示事件结束
        return true;
    }

//    private void chatSteam(String userContent, HttpResponse httpResponse) {
//        AtomicReference<SseEventType> reference = new AtomicReference<>();
//        reference.set(SseEventType.RUN_STARTED);
//        sendEvent(httpResponse, SseEventType.RUN_STARTED, "{}");
//
//        String messageId = UUID.randomUUID().toString();
//        streamChat(userContent, new StreamCallback() {
//
//            @Override
//            public void onReason(String reason) {
//                if (reference.get() != SseEventType.REASONING_CONTENT) {
//                    if (reference.get() == SseEventType.TEXT_MESSAGE_CONTENT) {
//                        sendEvent(httpResponse, SseEventType.TEXT_MESSAGE_END, String.format("{\"messageId\":\"%s\"}", messageId));
//                    }
//                    sendEvent(httpResponse, SseEventType.REASONING_START, "{}");
//                    reference.set(SseEventType.REASONING_CONTENT);
//                }
//                sendEvent(httpResponse, SseEventType.REASONING_CONTENT, String.format("{\"content\":%s}", json.toJson(reason)));
//            }
//
//            @Override
//            public void onToken(String token) {
//                if (reference.get() != SseEventType.TEXT_MESSAGE_CONTENT) {
//                    if (reference.get() == SseEventType.REASONING_CONTENT) {
//                        sendEvent(httpResponse, SseEventType.REASONING_END, "{}");
//                    }
//                    sendEvent(httpResponse, SseEventType.TEXT_MESSAGE_START, String.format("{\"messageId\":\"%s\"}", messageId));
//                    reference.set(SseEventType.TEXT_MESSAGE_CONTENT);
//                }
//
//                /*
//                 * 【安全说明】
//                 * token 中可能包含换行符、引号等特殊字符，
//                 * 必须正确转义为 JSON 字符串，否则会破坏 SSE data 格式。
//                 */
//                String res;
//                try {
//                    res = String.format("{\"messageId\":\"%s\",\"content\":%s}",
//                            messageId,
//                            json.toJson(token)
//                    );
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                sendEvent(httpResponse, SseEventType.TEXT_MESSAGE_CONTENT, res);
//            }
//
//            @Override
//            public void onComplete() {
//                reference.set(SseEventType.TEXT_MESSAGE_END);
//                // 3.4 文本消息结束
//                sendEvent(httpResponse, SseEventType.TEXT_MESSAGE_END, String.format("{\"messageId\":\"%s\"}", messageId));
//            }
//
//            @Override
//            public void onFinished() {
//                reference.set(SseEventType.RUN_FINISHED);
//                // 3.5 运行完成
//                sendEvent(httpResponse, SseEventType.RUN_FINISHED, "{}");
//            }
//
//            @Override
//            public void onError(Throwable error) {
//                String errMsg = error.getMessage();
//                if (errMsg == null) errMsg = "内部服务错误";
//                if (errMsg.length() > 200) errMsg = errMsg.substring(0, 200) + "...";
//                completeWithError(httpResponse, errorJson(errMsg));
//            }
//        });
//    }

    /**
     * 构建错误事件 JSON
     */
    private String errorJson(String message) {
        Map<String, String> obj = new HashMap<>();
        obj.put("code", "INTERNAL_ERROR");
        obj.put("message", message);
        try {
            return json.toJson(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
