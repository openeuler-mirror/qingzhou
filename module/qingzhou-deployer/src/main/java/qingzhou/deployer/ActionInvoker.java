package qingzhou.deployer;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.engine.ServiceInfo;

import java.util.Map;

public interface ActionInvoker extends ServiceInfo {
    @Override
    default boolean isAppShared() {
        return false;
    }

    Map<String, Response> invokeOnInstances(Request request, String... instances);

    Response invokeSingle(Request request);

    Map<String, Response> invoke(Request request);
}
