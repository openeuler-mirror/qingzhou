package qingzhou.deployer;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.util.Map;

public interface ActionInvoker {
    Map<String, Response> invokeOnInstances(Request request, String... instances);

    Response invokeSingle(Request request);

    Map<String, Response> invoke(Request request);
}
