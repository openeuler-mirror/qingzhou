package qingzhou.ai.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SseWatchdog {
    // 连续静默达到该时长后补发一次心跳
    private static final long SEND_INTERVAL_MS = 10_000L;
    // 会话总生命周期硬上限（兜底）：即使已产出内容、且上游流静默（既不 onComplete 也不 onError）、 客户端长连接又一直未断开，心跳会无限续期，使本会话的周期任务与响应对象始终无法回收；看门狗在此上限到达时主动收尾并自取消，杜绝“僵尸会话”空转。须大于 FIRST_CONTENT_TIMEOUT_MS。
    private static final long HARD_TIMEOUT_MS = 1_800_000L;
    // 仅用于“僵尸连接”兜底：模型正常思考慢/工具执行久时，心跳会持续续期、不会被误杀；是否继续等待主要由前端（展示计时）与用户（决定是否停止）控制，因此该阈值放得很宽。
    private static final long FIRST_CONTENT_TIMEOUT_MS = 600_000L;

    private static volatile ScheduledExecutorService watchdogExecutor;

    // 组件 deactivate 会 shutdown 线程池，重启后须重建，否则看门狗任务被静默丢弃
    static ScheduledExecutorService getExecutorService() {
        if (watchdogExecutor == null) {
            synchronized (SseWatchdog.class) {
                if (watchdogExecutor == null) {
                    watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "qz-ai-sse-watchdog");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return watchdogExecutor;
    }

    static ScheduledFuture<?> watch(SseChatListener sseChatListener) {
        return SseWatchdog.getExecutorService().scheduleWithFixedDelay(
                () -> watchdogTick(sseChatListener),
                SEND_INTERVAL_MS, SEND_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private static void watchdogTick(SseChatListener sse) {
        long now = System.currentTimeMillis();
        long elapsed = now - sse.startTime;
        if (elapsed >= HARD_TIMEOUT_MS) {
            // 会话总时长硬上限：无论是否已出内容、连接是否仍存活，都主动收尾并自取消，兜底回收看门狗，杜绝“已出内容后模型静默 + 客户端长连接不关”的无限心跳空转
            sse.finishWithError("MODEL_TIMEOUT", "会话运行已超过 30 分钟上限，已自动终止本次请求，请重试");
            return;
        }

        // 首字超时以会话开始时间为基准：心跳同样会刷新 lastEventTime，用 idleMs 判定将永远达不到阈值
        if (!sse.contentStarted && elapsed >= FIRST_CONTENT_TIMEOUT_MS) {
            // 已受理但长时间没有任何模型内容产出：判定无响应，主动收尾
            sse.finishWithError("MODEL_TIMEOUT", "模型长时间未响应，已终止本次请求，请重试");
            return;
        }

        long idleMs = now - sse.lastEventTime;
        if (idleMs > SEND_INTERVAL_MS) {
            try { // 心跳：告知前端“服务端还活着、仍在处理”，避免其把缓慢当卡死
                sse.sendEvent(SseEvent.of(SseEvent.Type.STATUS).stage("working"));
            } catch (RuntimeException e) {
                // 周期任务抛出即被 ScheduledExecutor 永久取消（异常由 Future 吞掉），心跳与超时兜底会一并失效，须就地收尾
                sse.logger.warn("sse push failed, session terminated: " + e.getMessage());
                sse.finishWithError("SERVER_ERROR", "响应推送失败，已终止本次请求");
            }
        }
    }
}
