package qingzhou.deployer;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.util.List;

public interface ActionInvoker {
    List<Response> invokeOnInstances(Request request, String... instances);

    Response invokeSingle(Request request);

    List<Response> invoke(Request request);
}
