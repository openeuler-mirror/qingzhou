package qingzhou.ai.impl;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ChatStage;
import qingzhou.llm.Listener;
import qingzhou.logger.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SSE 事件出口：负责把模型回调转为 text/event-stream 事件。
 *
 * 为保证“请求生命周期”对前端友好，这里额外做三件事：
 * 1. 统一事件出口并加锁：心跳线程与模型回调可能并发写连接，避免事件串包；
 * 2. 空闲心跳：一段时间没有模型事件时补发 STATUS(working)，让前端区分“仍在处理”与“连接已断开”；
 * 3. 看门狗：RUN_STARTED 之后长时间没有任何推理/正文/工具/用量等“内容类”事件，
 *    判定模型无响应主动以 RUN_ERROR(code=MODEL_TIMEOUT) 收尾；另设会话总时长硬上限，
 *    防止“已出内容但模型静默 + 客户端长连接不关”时心跳无限续期造成僵尸任务空转。
 */
public class SseListener implements Listener {
    private static final ScheduledExecutorService WATCHDOG_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "qingzhou-ai-sse-watchdog");
        t.setDaemon(true);
        return t;
    });

    /** 连续静默达到该时长后补发一次心跳 */
    private static final long HEARTBEAT_INTERVAL_MS = 10_000L;
    /**
     * 仅用于“僵尸连接”兜底：模型正常思考慢/工具执行久时，心跳会持续续期、不会被误杀；
     * 是否继续等待主要由前端（展示计时）与用户（决定是否停止）控制，因此该阈值放得很宽。
     */
    private static final long FIRST_CONTENT_TIMEOUT_MS = 600_000L;
    private static final String MODEL_TIMEOUT_MESSAGE = "模型长时间未响应，已终止本次请求，请重试";
    /**
     * 会话总生命周期硬上限（兜底）：即使已产出内容、且上游流静默（既不 onComplete 也不 onError）、
     * 客户端长连接又一直未断开，心跳会无限续期，使本会话的周期任务与响应对象始终无法回收；
     * 看门狗在此上限到达时主动收尾并自取消，杜绝“僵尸会话”空转。须大于 FIRST_CONTENT_TIMEOUT_MS。
     */
    private static final long HARD_TIMEOUT_MS = 1_800_000L;

    private static final Set<SseEvent.Type> CONTENT_EVENT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SseEvent.Type.REASONING_START, SseEvent.Type.REASONING_CONTENT, SseEvent.Type.REASONING_PAUSE,
            SseEvent.Type.TEXT_MESSAGE_START, SseEvent.Type.TEXT_MESSAGE_CONTENT,
            SseEvent.Type.TOOL_CALL, SseEvent.Type.USAGE)));

    private final HttpResponse httpResponse;
    private final Logger logger;
    private final Json json;

    private boolean isReasoning = false;
    private boolean isMessage = false;
    /** 已完成/已出错：之后到达的回调一律丢弃，避免连接关闭后模型继续刷屏 */
    private volatile boolean finished = false;
    /** 是否已产生过内容类事件（用于首字超时判定）；由看门狗线程无锁读取，须保证可见性 */
    private volatile boolean contentStarted = false;
    /** 最近一次事件写入时间：sendEvent 线程写、看门狗线程无锁读，须保证可见性 */
    private volatile long lastEventTime = System.currentTimeMillis();
    private final long startTime = System.currentTimeMillis();
    private ScheduledFuture<?> watchdogTask;
    private final Object sendLock = new Object();

    public SseListener(HttpResponse httpResponse, Logger logger, Json json) {
        this.httpResponse = httpResponse;
        this.logger = logger;
        this.json = json;
    }

    /** 请求已受理。由 AiChat 在技能匹配等耗时前置工作开始前调用，让客户端立即进入“正在思考” */
    public void sendStarted() {
        sendEvent(SseEvent.of(SseEvent.Type.RUN_STARTED));
        startWatchdog();
    }

    /** 阶段状态上报：模型实现把“等待中”细化（见 ChatStage），前端据此展示具体文案 */
    @Override
    public void onStatus(ChatStage stage) {
        if (stage == null) return;
        sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage(stage.name()));
    }

    private synchronized void startWatchdog() {
        stopWatchdog();
        watchdogTask = WATCHDOG_EXECUTOR.scheduleWithFixedDelay(
                this::watchdogTick, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopWatchdog() {
        if (watchdogTask != null) {
            watchdogTask.cancel(false);
            watchdogTask = null;
        }
    }

    private void watchdogTick() {
        if (finished) {
            stopWatchdog();
            return;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= HARD_TIMEOUT_MS) {
            // 会话总时长硬上限：无论是否已出内容、连接是否仍存活，都主动收尾并自取消，
            // 兜底回收看门狗，杜绝“已出内容后模型静默 + 客户端长连接不关”的无限心跳空转
            finishWithError("MODEL_TIMEOUT", "会话运行已超过 30 分钟上限，已自动终止本次请求，请重试");
            return;
        }
        long idleMs = System.currentTimeMillis() - lastEventTime;
        if (idleMs < HEARTBEAT_INTERVAL_MS) return; // 事件流正常，无需处理
        if (!contentStarted && idleMs >= FIRST_CONTENT_TIMEOUT_MS) {
            // 已受理但长时间没有任何模型内容产出：判定无响应，主动收尾
            finishWithError("MODEL_TIMEOUT", MODEL_TIMEOUT_MESSAGE);
            return;
        }
        // 心跳：告知前端“服务端还活着、仍在处理”，避免其把缓慢当卡死
        sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage(ChatStage.working.name()));
    }

    /** 统一事件出口：加锁避免并发写串包，并维护内容/活性状态 */
    private void sendEvent(SseEvent event) {
        synchronized (sendLock) {
            if (finished) return;
            if (CONTENT_EVENT_TYPES.contains(event.type)) contentStarted = true;
            lastEventTime = System.currentTimeMillis();
            httpResponse.send(toSseText(event));
        }
    }

    private void finishWithError(String code, String error) {
        synchronized (sendLock) {
            if (finished) return;
            finished = true;
            stopWatchdog();
            logger.error(code + ": " + error);
            try {
                httpResponse.sendFinish(toSseText(SseEvent.of(SseEvent.Type.RUN_ERROR).message(error).code(code)));
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
    public void onToolCall(String toolName) {
        sendEvent(SseEvent.of(SseEvent.Type.TOOL_CALL).toolName(toolName));
    }

    @Override
    public void onMessage(String content) {
        if (!isMessage) {
            // 切换为正文段：思考段的结束由前端收到 TEXT_MESSAGE_START 时自行收尾
            isMessage = true;
            isReasoning = false;
            sendEvent(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_START));
        }
        sendEvent(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_CONTENT).content(content));
    }

    @Override
    public void onUsage(int promptTokens, int completionTokens, int totalTokens) {
        logger.info("LLM usage: prompt=" + promptTokens + ", completion=" + completionTokens + ", total=" + totalTokens);
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
                httpResponse.sendFinish(toSseText(SseEvent.of(SseEvent.Type.RUN_FINISHED)));
            } catch (Exception e) {
                // 客户端已断开连接，无法发送结束事件，忽略
            }
        }
    }

    private String toSseText(SseEvent event) {
        String toJson;
        try {
            toJson = json.toJson(event.data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", event.type, toJson);
    }
}
