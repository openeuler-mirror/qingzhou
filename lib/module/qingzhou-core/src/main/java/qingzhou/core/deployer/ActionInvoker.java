package qingzhou.core.deployer;

import java.util.Map;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.engine.Service;

@Service(shareable = false)
public interface ActionInvoker {
    // 从应用所部署的实例中，任意选择一个来执行本次请求
    Response invokeAny(Request request);

    // 在指定的一个或多个实例上执行本次请求
    Map<String, Response> invokeAll(Request request, String... instances);

    // 根据当前请求的实例或集群等信息，自动选择在哪些实例上执行本次请求
    Map<String, Response> invokeAuto(Request request);
}
