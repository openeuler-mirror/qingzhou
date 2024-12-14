package qingzhou.core.deployer;

import java.util.Map;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.engine.Service;

@Service(shareable = false)
public interface ActionInvoker {
    // 检索请求的应用所安装到的所有实例，从其中自动选择一个来执行本次请求
    Response invokeOnce(Request request);

    // 检索请求的应用所安装到的所有实例，在所有的实例上执行本次请求
    Map<String, Response> invokeMultiple(Request request, String... instances);

    // 检索请求的应用所安装到的所有实例和请求的 ModelAction，若 ModelAction 的 distribute 为真则在所有的实例上执行本次请求，否则从所有的实例中自动选择一个来执行本次请求
    Map<String, Response> invokeIfDistribute(Request request);
}
