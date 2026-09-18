package qingzhou.registry;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;

/**
 * 需要应用实现的服务接口，Console 通过此接口与应用交互
 */
public interface AppStub {
    AppMeta getAppMeta();

    void invokeApp(RequestImpl request) throws Throwable;

    // 携带调用者角色的执行入口：终端用户路径必须经此进入，由实现方做三级角色判定；不带角色的入口保留给实例间等内部调用
    default void invokeApp(RequestImpl request, String[] roles) throws Throwable {
        invokeApp(request);
    }
}
