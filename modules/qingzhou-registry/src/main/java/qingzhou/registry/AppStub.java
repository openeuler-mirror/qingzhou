package qingzhou.registry;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;

/**
 * 需要应用实现的服务接口，Console 通过此接口与应用交互
 */
public interface AppStub {
    AppMeta getAppMeta();

    void invokeApp(RequestImpl request) throws Throwable;

    /**
     * 携带调用者角色的执行入口：终端用户路径（HTTP 入口、AI / MCP 工具通道）必须经此进入，
     * 由实现方按应用、模块、动作三级角色判定；角色的来源必须是服务端鉴权结果，不得取自请求参数。
     * 不带角色的 invokeApp(request) 保留给实例间等内部可信调用。
     */
    default void invokeApp(RequestImpl request, String[] roles) throws Throwable {
        invokeApp(request);
    }
}
