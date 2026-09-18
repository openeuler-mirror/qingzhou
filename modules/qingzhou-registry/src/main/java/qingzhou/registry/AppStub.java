package qingzhou.registry;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;

/**
 * 需要应用实现的服务接口，Console 通过此接口与应用交互
 */
public interface AppStub {
    AppMeta getAppMeta();

    void invokeApp(RequestImpl request) throws Throwable;
}
