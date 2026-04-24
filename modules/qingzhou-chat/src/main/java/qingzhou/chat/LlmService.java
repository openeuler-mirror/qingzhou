package qingzhou.chat;

import com.agentsflex.core.message.ToolCall;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Parameter;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.core.util.ArrayUtil;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.google.gson.GsonBuilder;
import qingzhou.api.Constants;
import qingzhou.api.InputType;
import qingzhou.api.Lang;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.I18nService;
import qingzhou.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class LlmService {

    private final Registry registry;
    private final I18nService i18nService;
    private final ChatModel chatModel;

    private List<Tool> tools;

    public LlmService(Registry registry, I18nService i18nService, ChatModel chatModel) {
        this.registry = registry;
        this.i18nService = i18nService;
        this.chatModel = chatModel;
    }

    public void streamChat(String message, StreamCallback callback) {
        MemoryPrompt prompt = new MemoryPrompt();
        prompt.addMessage(new UserMessage(message));
        if (tools == null) {
            tools = getTools();
            System.out.println("添加了 " + tools.size() + " 个Tool。");
        }
        prompt.addTools(tools);
        if (chatModel == null) {
            throw new RuntimeException("qingzhou-chat is not configured.");
        }
        chatModel.chatStream(prompt, new StreamResponseListener() {

            @Override
            public void onMessage(StreamContext context, AiMessageResponse response) {
                String reasoningContent = response.getMessage().getReasoningContent();
                if (StringUtil.hasText(reasoningContent)) {
                    callback.onReason(reasoningContent);
                }
                String content = response.getMessage().getContent();
                if (content != null) {
                    callback.onToken(content);
                }

                if (response.getMessage().isFinalDelta() && response.getMessage().getToolCalls() != null) {
                    prompt.addMessage(response.getMessage());
                    for (ToolCall toolCall : response.getMessage().getToolCalls()) {
                        System.out.println(">>>>> " + toolCall.getName() + ": " + JSON.toJSONString(toolCall.getArgsMap(), JSONWriter.Feature.PrettyFormat));
                    }
                    List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
                    prompt.addMessages(toolMessages);

                    chatModel.chatStream(prompt, this);
                } else if (response.getMessage().isFinalDelta() && !response.getMessage().hasToolCalls()) {
                    // 保存完整回复到记忆
                    prompt.addMessage(response.getMessage());
                    callback.onComplete();
                    callback.onFinished();
                }
            }

            @Override
            public void onFailure(StreamContext context, Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }


    private static final String[] allowAction = new String[]{
            Show.ACTION_CODE_SHOW,
            qingzhou.api.type.List.ACTION_CODE_LIST,
            Monitor.ACTION_CODE_MONITOR
    };

    private List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        List<String> allLocalApps = registry.getAllLocalApps();
        for (String appCode : allLocalApps) {
            AppStubLocal appStubLocal = registry.getLocalApp(appCode);
            App app = appStubLocal.getAppMeta().getApp();
            for (Model model : app.models) {
                for (ModelAction action : model.actions) {
                    String methodName = action.code;
                    if (!ArrayUtil.contains(allowAction, methodName)) {
                        continue;
                    }
                    String code = model.code;
                    String description = "[GROUP:" + i18nService.getI18n(app.name, Lang.zh) + "]" + i18nService.getI18n(model.name, Lang.zh) + "模块" + i18nService.getI18n(action.name, Lang.zh) + "方法, " + i18nService.getI18n(action.info, Lang.zh);
                    Tool.Builder builder = Tool.builder()
                            .name(appCode + "." + methodName + "_" + code)
                            .description(description);
                    if (Show.ACTION_CODE_SHOW.equals(methodName)) {
                        for (ModelField field : model.fields) {
                            if (field.id) {
                                builder.addParameter(Parameter.builder()
                                        .name(field.fieldName)
                                        .description(i18nService.getI18n(field.name, Lang.zh))
                                        .type(field.input_type == InputType.number ? "number" : "string")
                                        .required(true)
                                        .build());
                            }
                        }
                    }
                    Tool tool = builder
                            .function(args -> {
                                try {
                                    RequestImpl request = new RequestImpl();
                                    request.setApp(appCode);
                                    request.setModel(code);
                                    request.setAction(methodName);
                                    request.setInstance(Constants.LOCAL_INSTANCE_ID);
                                    args.forEach((s, o) -> request.getParameters().put(s, o.toString()));
                                    appStubLocal.invokeApp(request);
                                    return request.getResponse().getData();
                                } catch (Throwable e) {
                                    return "执行异常：" + e.getMessage();
                                }
                            })
                            .build();
                    tools.add(tool);
                }
            }
        }
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(tools));
        return tools;
    }
}