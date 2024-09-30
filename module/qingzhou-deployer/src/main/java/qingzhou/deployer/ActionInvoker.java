package qingzhou.deployer;

import java.util.List;

import qingzhou.api.Request;
import qingzhou.api.Response;

public interface ActionInvoker {
    List<Response> invokeOnInstances(Request request, String... instances);

    Response invokeSingle(Request request);

    List<Response> invokeAll(Request request);
}
