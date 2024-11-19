package qingzhou.core.deployer;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.engine.Service;

import java.util.Map;

@Service(shareable = false)
public interface ActionInvoker {
    Map<String, Response> invokeOnInstances(Request request, String... instances);

    Response invokeSingle(Request request);

    Map<String, Response> invoke(Request request);
}
