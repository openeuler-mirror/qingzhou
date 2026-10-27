package qingzhou.ai.impl;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Listener;
import qingzhou.llm.impl.Utils;
import qingzhou.logger.Logger;

/**
 * SSE 事件出口：负责把模型回调转为 text/event-stream 事件。
 * <p>
 * 为保证“请求生命周期”对前端友好，这里额外做三件事：
 * 1. 统一事件出口并加锁：心跳线程与模型回调可能并发写连接，避免事件串包；
 * 2. 空闲心跳：一段时间没有模型事件时补发 STATUS(working)，让前端区分“仍在处理”与“连接已断开”；
 * 3. 看门狗：RUN_STARTED 之后长时间没有任何推理/正文/工具/用量等“内容类”事件，
 * 判定模型无响应主动以 RUN_ERROR(code=MODEL_TIMEOUT) 收尾；另设会话总时长硬上限，
 * 防止“已出内容但模型静默 + 客户端长连接不关”时心跳无限续期造成僵尸任务空转。
 */
public class SseListener implements Listener {
    private static volatile ScheduledExecutorService watchdogExecutor = newWatchdogExecutor();

    private static ScheduledExecutorService newWatchdogExecutor() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "qz-ai-sse-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    // 组件 deactivate 会 shutdown 线程池，重启后须重建，否则看门狗任务被静默丢弃
    static ScheduledExecutorService watchdog() {
        ScheduledExecutorService executor = watchdogExecutor;
        if (executor.isShutdown()) {
            synchronized (SseListener.class) {
                if (watchdogExecutor.isShutdown()) {
                    watchdogExecutor = newWatchdogExecutor();
                }
                executor = watchdogExecutor;
            }
        }
        return executor;
    }

    // 连续静默达到该时长后补发一次心跳
    private static final long HEARTBEAT_INTERVAL_MS = 10_000L;
    // 会话总生命周期硬上限（兜底）：即使已产出内容、且上游流静默（既不 onComplete 也不 onError）、 客户端长连接又一直未断开，心跳会无限续期，使本会话的周期任务与响应对象始终无法回收；看门狗在此上限到达时主动收尾并自取消，杜绝“僵尸会话”空转。须大于 FIRST_CONTENT_TIMEOUT_MS。
    private static final long HARD_TIMEOUT_MS = 1_800_000L;
    // 仅用于“僵尸连接”兜底：模型正常思考慢/工具执行久时，心跳会持续续期、不会被误杀；是否继续等待主要由前端（展示计时）与用户（决定是否停止）控制，因此该阈值放得很宽。
    private static final long FIRST_CONTENT_TIMEOUT_MS = 600_000L;

    private static final Set<SseEvent.Type> CONTENT_EVENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SseEvent.Type.REASONING_START, SseEvent.Type.REASONING_CONTENT, SseEvent.Type.REASONING_PAUSE,
            SseEvent.Type.TEXT_MESSAGE_START, SseEvent.Type.TEXT_MESSAGE_CONTENT,
            SseEvent.Type.TOOL_CALL, SseEvent.Type.USAGE)));

    private final HttpResponse httpResponse;
    private final Logger logger;
    private final Json json;

    private boolean isReasoning = false;
    private boolean isMessage = false;
    /**
     * 已完成/已出错：之后到达的回调一律丢弃，避免连接关闭后模型继续刷屏
     */
    private volatile boolean finished = false;
    /**
     * 客户端已断开：置位后模型侧停止拉取（见 Listener#isCancelled）
     */
    private volatile boolean cancelled = false;
    /**
     * 是否已产生过内容类事件（用于首字超时判定）；由看门狗线程无锁读取，须保证可见性
     */
    private volatile boolean contentStarted = false;
    /**
     * 最近一次事件写入时间：sendEvent 线程写、看门狗线程无锁读，须保证可见性
     */
    private volatile long lastEventTime = System.currentTimeMillis();
    private volatile long startTime;
    private ScheduledFuture<?> watchdogTask;
    private final Object sendLock = new Object();

    final StringBuilder allContent = new StringBuilder();

    /**
     * 流正常结束后的动作（AiChat 落库回答全文：chat() 异步，全文须等 onComplete 才完整）
     */
    private Runnable completeAction;

    public SseListener(HttpResponse httpResponse, Logger logger, Json json) {
        this.httpResponse = httpResponse;
        this.logger = logger;
        this.json = json;
    }

    public void onCompleteAction(Runnable action) {
        this.completeAction = action;
    }

    /**
     * 请求已受理。由 AiChat 在技能匹配等耗时前置工作开始前调用，让客户端立即进入“正在思考”。
     * assistantMessageId 为本轮 AI 回复预生成的落库 id，随 RUN_STARTED 下发供前端回填
     */
    public synchronized void setStarted(String conversationId, String assistantMessageId) { // 与 stopWatchdog 同锁，消除取消竞态窗口
        startTime = System.currentTimeMillis();
        watchdogTask = watchdog().scheduleWithFixedDelay(
                this::watchdogTick,
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        sendEvent(SseEvent.of(SseEvent.Type.RUN_STARTED).conversationId(conversationId).messageId(assistantMessageId));
    }

    /**
     * 用户提问落库后下发其消息 id（USER_MESSAGE）：前端问答成对删除的远程对齐依赖此 id
     */
    public void sendUserMessage(String userMessageId) {
        sendEvent(SseEvent.of(SseEvent.Type.USER_MESSAGE).userMessageId(userMessageId));
    }

    private void watchdogTick() {
        if (finished) {
            stopWatchdog();
            return;
        }

        // 客户端已断开且模型侧可能不再回调：主动收尾，回收心跳与响应对象（正文已由取消路径落库）
        if (cancelled) {
            finished = true;
            stopWatchdog();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= HARD_TIMEOUT_MS) {
            // 会话总时长硬上限：无论是否已出内容、连接是否仍存活，都主动收尾并自取消，兜底回收看门狗，杜绝“已出内容后模型静默 + 客户端长连接不关”的无限心跳空转
            finishWithError("MODEL_TIMEOUT", "会话运行已超过 30 分钟上限，已自动终止本次请求，请重试");
            return;
        }

        long idleMs = System.currentTimeMillis() - lastEventTime;
        if (idleMs < HEARTBEAT_INTERVAL_MS) return; // 事件流正常，无需处理

        // 首字超时以会话开始时间为基准：心跳同样会刷新 lastEventTime，用 idleMs 判定将永远达不到阈值
        if (!contentStarted && elapsed >= FIRST_CONTENT_TIMEOUT_MS) {
            // 已受理但长时间没有任何模型内容产出：判定无响应，主动收尾
            finishWithError("MODEL_TIMEOUT", "模型长时间未响应，已终止本次请求，请重试");
            return;
        }

        onStillActive();
    }

    @Override
    public void onSkillMatching() {
        sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage("matching"));
    }

    @Override
    public void onStillActive() {
        // 心跳：告知前端“服务端还活着、仍在处理”，避免其把缓慢当卡死
        sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage("working"));
    }

    private synchronized void stopWatchdog() {
        if (watchdogTask != null) {
            watchdogTask.cancel(false);
            watchdogTask = null;
        }
    }

    /**
     * 统一事件出口：加锁避免并发写串包，并维护内容/活性状态
     */
    private void sendEvent(SseEvent event) {
        synchronized (sendLock) {
            if (finished || cancelled) return;
            if (CONTENT_EVENT_TYPES.contains(event.type)) contentStarted = true;
            lastEventTime = System.currentTimeMillis();
            try {
                httpResponse.send(toSseText(event));
            } catch (IllegalStateException e) {
                if (!httpResponse.isCancelled()) throw e; // 背压满等异常保持原有上抛行为
                cancelled = true; // 客户端断开：通知模型停止拉取
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private void finishWithError(String code, String error) {
        synchronized (sendLock) {
            if (finished) return;
            finished = true;
            stopWatchdog();
            try {
                httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.RUN_ERROR).message(error).code(code))).finish();
            } catch (Exception e) {
                // 客户端已断开连接，无法发送错误信息，忽略
            }
        }
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
    private static String uiActionOf(String toolName) {
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
            allContent.append(content);
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
        synchronized (sendLock) {
            if (finished) return;
            finished = true;
            stopWatchdog();
            try {
                httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.RUN_FINISHED))).finish();
            } catch (Exception e) {
                // 客户端已断开连接，无法发送结束事件，忽略
            }
        }
        // 客户端在首字前即断开：无内容可落库，跳过以免留下空回答
        if (completeAction != null && (!cancelled || allContent.length() > 0)) {
            try {
                completeAction.run();
            } catch (Throwable t) {
                // 落库失败仅记日志，不影响流已正常结束
                logger.warn("failed to persist assistant message: " + t.getMessage());
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
}
