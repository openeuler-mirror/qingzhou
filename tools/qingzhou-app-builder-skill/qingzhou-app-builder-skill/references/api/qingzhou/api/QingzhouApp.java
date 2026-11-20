package qingzhou.api;

import qingzhou.api.annotations.App;

/**
 * 应用入口。实现类须同时标注 {@link App}，平台在构建期记录其类名，运行期反射创建唯一实例后驱动启动。
 * <p>
 * 启动顺序：创建本实例 → 创建各模块实例并注入 {@link AppContext} → {@link #start(AppContext)}
 * → 逐模块 {@link ModelBase#start()} → 注册服务对外提供请求；停止时逆序。
 */
public interface QingzhouApp {
    /**
     * 应用初始化。此时模块实例已创建但尚未 start，适合做跨模块的全局准备，
     * 或直接通过 {@link AppContext} 获取配置与平台服务。
     */
    void start(AppContext appContext) throws Exception;

    /**
     * 应用停止回调，在各模块 {@link ModelBase#stop()} 之后调用。
     */
    default void stop() {
    }
}
