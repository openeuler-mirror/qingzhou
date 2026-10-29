package qingzhou.ai.impl;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ChatListener;
import qingzhou.llm.impl.Utils;
import qingzhou.logger.Logger;

class SseChatListener implements ChatListener {
    private final Set<SseEvent.Type> CONTENT_EVENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SseEvent.Type.REASONING_START, SseEvent.Type.REASONING_CONTENT, SseEvent.Type.REASONING_PAUSE,
            SseEvent.Type.TEXT_MESSAGE_START, SseEvent.Type.TEXT_MESSAGE_CONTENT,
            SseEvent.Type.TOOL_CALL, SseEvent.Type.USAGE)));

    private final HttpResponse httpResponse;
    final Logger logger;
    private final Json json;

    private Subscription subscription;

    volatile long startTime;
    volatile long lastEventTime;
    private ScheduledFuture<?> watchdogTask;
    private MessageAggregator messageAggregator;

    private final Object sendLock = new Object();
    // 已完成/已出错：之后到达的回调一律丢弃，避免连接关闭后模型继续刷屏
    private volatile boolean finished = false;

    // 是否已产生过内容类事件（用于首字超时判定）；由看门狗线程无锁读取，须保证可见性
    volatile boolean contentStarted = false;
    private boolean isReasoning = false;
    private boolean isMessage = false;

    SseChatListener(HttpResponse httpResponse, Logger logger, Json json) {
        this.httpResponse = httpResponse;
        this.logger = logger;
        this.json = json;
    }

    void setStarted(String conversationId, String assistantMessageId, MessageAggregator messageAggregator) {
        this.startTime = System.currentTimeMillis();
        this.lastEventTime = System.currentTimeMillis();
        this.watchdogTask = SseWatchdog.watch(this);
        this.messageAggregator = messageAggregator;

        // 先告知"已受理"：技能匹配等前置工作可能耗时数秒，不能让客户端误以为请求没发出去
        sendEvent(SseEvent.of(SseEvent.Type.RUN_STARTED).conversationId(conversationId).messageId(assistantMessageId));
    }

    /**
     * 用户提问落库后下发其消息 id（USER_MESSAGE）：前端问答成对删除的远程对齐依赖此 id
     */
    void sendUserMessage(String userMessageId) {
        sendEvent(SseEvent.of(SseEvent.Type.USER_MESSAGE).userMessageId(userMessageId));
    }

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
    }

    @Override
    public void onSkillMatch() {
        sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage("matching"));
    }

    void finishWithError(String code, String error) {
        finish(SseEvent.of(SseEvent.Type.RUN_ERROR).message(error).code(code));
    }

    @Override
    public void onReasoning(String content) {
        if (!isReasoning) {
            // 切换为思考段：正文段的结束由前端收到 REASONING_START 时自行收尾
            isReasoning = true;
            isMessage = false;
            sendEvent(SseEvent.of(SseEvent.Type.REASONING_START));
        }
        sendEvent(SseEvent.of(SseEvent.Type.REASONING_CONTENT).content(content));
    }

    @Override
    public void onReasoningPause() {
        // 工具开始执行：前端据此展示“执行中”占位，工具结束后由 TOOL_CALL 补全结果
        sendEvent(SseEvent.of(SseEvent.Type.REASONING_PAUSE));
    }

    @Override
    public void onToolCall(String toolCallId, String toolName, String args) {
        // 补发 args（注入 actionCode 后的 JSON）：前端联动完全由 args 驱动，旧前端忽略该字段
        sendEvent(SseEvent.of(SseEvent.Type.TOOL_CALL).toolName(toolName).content(enrichActionArgs(toolName, args)));
    }

    /**
     * 把 UI 联动动作码并入工具入参 JSON，使前端联动完全由 args 驱动：
     * - qingzhou_action_<动作码>（RegisterActionTools.TOOL_NAME_PREFIX 命名约定）→ 注入 actionCode=<动作码>
     * - AppListTool（应用列表查询，未声明 TOOL_NAME 时工具名即组件类名，见 LlmConverter）→ 注入 actionCode=appList
     * args 非法/非对象时原样透传（此时前端无联动，属正常降级）
     */
    private String enrichActionArgs(String toolName, String args) {
        String actionCode = uiActionOf(toolName);
        if (actionCode == null) return args;
        try {
            Map<String, Object> map = args == null || args.isEmpty() ? new HashMap<>() : json.fromJson(args, HashMap.class);
            if (map == null) return args;
            map.put("actionCode", actionCode);
            return json.toJson(map);
        } catch (Exception e) {
            return args;
        }
    }

    /**
     * 工具名 → UI 联动动作码；返回 null 表示无联动。
     * 同语义的新增工具无需改动（前缀/类名约定覆盖），仅新增语义动作时才需要在此补充分支。
     * 注意：qingzhou_action_ 前缀须与 RegisterActionTools.TOOL_NAME_PREFIX 保持一致（跨模块各自定义）
     */
    private String uiActionOf(String toolName) {
        final String prefix = "qingzhou_action_";
        if (toolName != null && toolName.startsWith(prefix)) {
            switch (toolName.substring(prefix.length())) {
                case "page":
                    return "page";
                case "show":
                    return "show";
                case "monitor":
                    return "monitor";
                // 空间切换（非写操作）：联动聚焦到该模型的列表页（空间列表所在），已打开则强制刷新。
                // currentspace 为纯查询、无状态变化，不联动
                case "switchspace":
                    return "focus";
                // 写操作（add/update/delete）不联动：AI 写数据时驱动左侧视图可能与用户当前操作产生数据冲突，联动范围由后端统一控制
                default:
                    return null;
            }
        }
        // 元数据查询工具（未声明 TOOL_NAME 时工具名即组件类名，见 LlmConverter）
        if ("AppListTool".equals(toolName)) return "appList";
        if ("ModelTool".equals(toolName)) return "page";
        return null;
    }

    @Override
    public void onMessage(String content) {
        if (!isMessage) {
            // 切换为正文段：思考段的结束由前端收到 TEXT_MESSAGE_START 时自行收尾
            isMessage = true;
            isReasoning = false;
            sendEvent(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_START));
        }
        if (content != null) {
            sendEvent(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_CONTENT).content(content));

            if (messageAggregator != null) {
                messageAggregator.allContent.append(content);
            }
        } else {
            sendEvent(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_CONTENT));
        }
    }

    @Override
    public void onUsage(int promptTokens, int completionTokens, int totalTokens) {
        sendEvent(SseEvent.of(SseEvent.Type.USAGE).usage(promptTokens, completionTokens, totalTokens));
    }

    @Override
    public void onError(String error) {
        finishWithError("MODEL_ERROR", error);
    }

    @Override
    public void onComplete() {
        finish(SseEvent.of(SseEvent.Type.RUN_FINISHED));

        // 客户端在首字前即断开：无内容可落库，跳过以免留下空回答
        if (messageAggregator != null) {
            if (messageAggregator.allContent.length() > 0) {
                messageAggregator.aggregatedMessage(messageAggregator.allContent.toString());
                messageAggregator.allContent.setLength(0);
            }
        }
    }

    private void finish(SseEvent event) {
        synchronized (sendLock) {
            if (finished) return;
            finished = true;

            if (watchdogTask != null) {
                watchdogTask.cancel(false);
                watchdogTask = null;
            }

            try {
                httpResponse.send(toSseText(event));
            } catch (Exception e) {
                // 客户端已断开或缓冲已满，错误事件发不出去
            } finally {
                httpResponse.finish(); // 未送达也必须终止响应流，否则订阅方悬挂、连接与线程无法回收
            }
        }
    }

    /**
     * 统一事件出口：加锁避免并发写串包，并维护内容/活性状态
     */
    void sendEvent(SseEvent event) {
        synchronized (sendLock) {
            if (finished) return;

            if (CONTENT_EVENT_TYPES.contains(event.type)) {
                contentStarted = true;
            }

            lastEventTime = System.currentTimeMillis();

            try {
                httpResponse.send(toSseText(event));
            } catch (Throwable e) { // 客户端已断开：置位后模型侧停止拉取
                finished = true; // 标记结束
                if (subscription != null) {
                    subscription.cancel(); // 客户端断开：通知模型停止拉取
                }
            }
        }
    }

    private String toSseText(SseEvent event) {
        String toJson;
        try {
            toJson = json.toJson(event.data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            toJson = "serialize event failed: " + Utils.errorMessage(e); // getMessage 可能为 null，兜底固定错误串
        }
        return String.format("event: %s\ndata: %s\n\n", event.type, toJson);
    }

    static abstract class MessageAggregator {
        private final StringBuilder allContent = new StringBuilder();

        abstract void aggregatedMessage(String content);
    }
}
