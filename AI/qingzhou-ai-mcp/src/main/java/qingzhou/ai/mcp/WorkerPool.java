package qingzhou.ai.mcp;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import qingzhou.common.Configs;

// 工具执行线程池：网络 IO 线程只投递，解析、调用与写响应都在这里完成
class WorkerPool {
    private final ThreadPoolExecutor executor;
    private final long queueTimeoutNanos;

    WorkerPool(Configs configs) {
        int threads = (int) configs.between("mcp_worker_threads", 1, 64, 16);
        int capacity = (int) configs.between("mcp_worker_queue", 1, 10_000, 100);
        queueTimeoutNanos = TimeUnit.SECONDS.toNanos(configs.between("mcp_queue_timeout_seconds", 1, 600, 60));

        AtomicInteger seq = new AtomicInteger();
        executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), runnable -> {
            Thread thread = new Thread(runnable, "qz-mcp-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    // 池满即拒绝：由调用方回 503，不在 IO 线程上排队以免拖垮同线程上的其他连接
    boolean submit(Runnable task) {
        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    // 排队过久的请求已无意义：工具通常有超时预算，放行只会让客户端空等
    boolean isExpired(long enqueuedAt) {
        return System.nanoTime() - enqueuedAt > queueTimeoutNanos;
    }

    // 排空而非中断：丢弃排队请求会使其 finish() 永不执行，响应挂到空闲超时
    boolean terminate() throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
