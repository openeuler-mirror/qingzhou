package qingzhou.deployer.impl;

import qingzhou.api.Request;
import qingzhou.api.Response;

interface ActionMethod {
    void invoke(Request request, Response response) throws Exception;
}
