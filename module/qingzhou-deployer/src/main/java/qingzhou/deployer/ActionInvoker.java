package qingzhou.deployer;

import qingzhou.api.Request;
import qingzhou.api.Response;

import java.util.List;

public interface ActionInvoker {
    List<Response> invokeOnInstances(Request request, String[] instances) throws Exception;

    Response invokeOnce(Request request) throws Exception;

    List<Response> invokeAuto(Request request) throws Exception;
}
