package qingzhou.api;

public interface QingzhouApp {
    /**
     * 用于判断当前环境是否具备应用的运行条件，并据此触发应用的生命周期：
     * 条件满足（true）时调用 start，不满足（false）时调用 stop。
     * 注意：因轻舟框架会周期性调用该方法，应用的 start 和 stop 会被反复执行，请确保相关逻辑的幂等性。
     */
    default boolean available(AppContext appContext) {
        return true;
    }

    void start(AppContext appContext) throws Exception;

    default void stop() {
    }
}
