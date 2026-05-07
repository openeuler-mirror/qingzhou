package qingzhou.ai;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.Constants;
import qingzhou.api.Lang;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.App;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.*;
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
    private Collection<Tool> tools;

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
        httpResponse.contentTypeJsonUtf8();// 返回内容是字符串，非二进制流
        chatModel.generate(message, tools(), new Listener() {
            final String messageId = UUID.randomUUID().toString().replace("-", "");
            boolean isReasoning = false;
            boolean isMessage = false;

            void sendEvent(String event, String data) {
                httpResponse.send("event: " + event + "\ndata: " + data + "\n\n");
            }

            String toJson(String messageId, String content) {
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

            @Override
            public void onBegin() {
                sendEvent("RUN_STARTED", "{}");
            }

            @Override
            public void onReasoning(String content) {
                if (!isReasoning) {
                    isReasoning = true;
                    if (isMessage) {
                        sendEvent("TEXT_MESSAGE_END", toJson(messageId, null));
                    }
                    sendEvent("REASONING_START", "{}");
                }
                sendEvent("REASONING_CONTENT", toJson(null, content));
            }

            @Override
            public void onMessage(String content) {
                if (!isMessage) {
                    isMessage = true;
                    if (isReasoning) {
                        sendEvent("REASONING_END", "{}");
                    }
                    sendEvent("TEXT_MESSAGE_START", toJson(messageId, null));
                }
                sendEvent("TEXT_MESSAGE_CONTENT", toJson(messageId, content));
            }

            @Override
            public void onError(Throwable t) {
                String errMsg = t.getMessage();
                logger.error(errMsg);
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

    public void sendEventFinish(HttpResponse writer, String error) {
        String errorJson = "{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + error + "\"}";
        writer.sendFinish("event: RUN_ERROR" + "\ndata: " + errorJson + "\n\n");
    }

    private Collection<Tool> tools() {
        if (tools == null) {
            tools = new HashSet<>();
            tools.add(webIndexTools());
            tools.add(webAppMetaTools());
            tools.add(webModelMetaTools());
            tools.add(invokeTool());
        }
        return tools;
    }

    private Tool webIndexTools() {
        StringBuilder langParameterDescription = new StringBuilder("指定以哪种国际化语言展示结果，");
        List<String> langList = new ArrayList<>();
        for (Lang lang : Lang.values()) {
            langParameterDescription.append(lang.flag).append(": ").append(lang.info);
            langList.add(lang.flag);
        }
        return Tool.of("webIndex",
                "该接口是轻舟平台的应用资产列表查询接口，用于查询环境中已部署的所有业务及中间件应用信息，返回每条应用的名称、部署服务器 IP 和功能简介等信息，可支撑前端展示应用清单、资产管理、服务盘点等场景。",
                new ToolParameter[]{ToolParameter.of(Constants.REQUEST_PARAMETER_NAME_LANG, langParameterDescription.toString(), ParameterType.STRING, false, langList.toArray(new String[0]))},
                new Function<Object[], Object>() {
                    @Override
                    public Object apply(Object[] objects) {
                        String langValue;
                        if (objects != null && objects.length > 0 && objects[0] instanceof Map) {
                            Map<String, String> params = (Map<String, String>) objects[0];
                            langValue = params.get(Constants.REQUEST_PARAMETER_NAME_LANG);
                        } else {
                            langValue = Lang.zh.flag;
                        }
                        List<Map<String, String>> appInfoList = new ArrayList<>();
                        for (String localApp : registry.getAllLocalApps()) {
                            appInfoList.add(appInfo(registry.getLocalInstance(), registry.getLocalApp(localApp).getAppMeta().getApp(), langValue));
                        }
                        registry.getAllRemoteInstances().forEach(instance -> {
                            InstanceInfo remoteInstance = registry.getRemoteInstance(instance);
                            registry.getAllRemoteApps(instance).forEach(appCode -> {
                                App app = registry.getRemoteApp(instance, appCode).getAppMeta().getApp();
                                appInfoList.add(appInfo(remoteInstance, app, langValue));
                            });
                        });
                        try {
                            return json.toJson(appInfoList);
                        } catch (Exception e) {
                            return "{" +
                                    "  \"code\": \"500\"," +
                                    "  \"msg\": \"" + e.getMessage() + "\"," +
                                    "  \"success\": false" +
                                    "}";
                        }
                    }

                    private Map<String, String> appInfo(InstanceInfo instanceInfo, App app, String lang) {
                        Map<String, String> appInfo = new HashMap<>();
                        appInfo.put("instanceId", instanceInfo.getId());
                        appInfo.put("instanceHost", instanceInfo.getHost());
                        appInfo.put("code", app.code);
                        appInfo.put("name", i18nService.getI18n(app.name, lang));
                        appInfo.put("info", i18nService.getI18n(app.info, lang));
                        return appInfo;
                    }
                });
    }

    private Tool webAppMetaTools() {
        return null;// todo
    }

    private Tool webModelMetaTools() {
        return null;// todo
    }

    private Tool invokeTool() {
        return null;// todo
    }
}
