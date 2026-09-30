package qingzhou.http.server.util;

/**
 * 请求分发链上的异常展开与回调收敛工具。
 */
public final class Errors {
    // Reactor 包装通常 2~3 层，上限仅兜底循环引用的异常链
    private static final int MAX_CAUSE_DEPTH = 8;

    // 入参须为最内层 cause：Reactor 会包装异常，调用方先用 getCause 展开再传入，避免重复遍历。
    // 自环（t.getCause() == t）之外还存在 A->B->A 的长环，逐层下钻会永不终止；深度上限兜底，
    // EventLoop 上死循环的代价是该 reactor 全部连接挂死，截断在浅层不影响 statusOf 的类型判断
    public static Throwable getCause(Throwable e) {
        for (int depth = 0; depth < MAX_CAUSE_DEPTH
                && e.getCause() != null && e.getCause() != e; depth++) {
            e = e.getCause();
        }
        return e;
    }

    // 回调可抛 Throwable（含 OSGi 刷 bundle 时的 NoClassDefFoundError），一律收敛为返回值，避免中断收尾链
    public static Throwable safeRun(Callback callback) {
        try {
            callback.run();
            return null;
        } catch (Throwable e) {
            return getCause(e);
        }
    }

    public interface Callback {
        void run() throws Throwable;
    }
}
